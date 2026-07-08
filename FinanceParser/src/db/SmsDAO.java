package db;

import parser.*;
import model.Transaction;
import java.sql.*;

// SmsDAO — manages the complete SMS processing pipeline:
// 1. Store raw SMS (parse_status='pending')
// 2. Parse it (AxisSmsParser or BankSmsParser)
// 3. Auto-categorize merchant
// 4. Insert TRANSACTION record
// 5. Update BANK_SMS with all parsed fields + link to TRANSACTION

public class SmsDAO {

    // ── AXIS BANK MULTI-LINE SMS ──────────────────────────────────
    public void ingestAxisSms(String rawSms, int userId, int sourceId) {
        int smsId = insertRawSms(rawSms);
        if (smsId < 0) return;

        AxisSmsParser.SmsResult r = AxisSmsParser.parse(rawSms);
        if (!r.success) { markFailed(smsId, r.failureReason); return; }

        int categoryId = CategoryEngine.getCategoryId(r.merchantClean);
        TransactionDAO txDao = new TransactionDAO();
        int merchantId = txDao.findOrCreateMerchant(r.merchantClean);

        Transaction t = new Transaction(userId, merchantId, categoryId, sourceId,
                r.amount, r.txDate, r.currency,
                r.txType.equals("CREDIT") ? "credit" : "parsed", rawSms);
        int txId = txDao.insertTransaction(t);

        updateBankSmsAxis(smsId, r, txId);
        System.out.println("[SmsDAO] Axis → " + r);
    }

    // ── OTHER BANKS (HDFC, SBI, ICICI, KOTAK etc.) ───────────────
    public void ingestBankSms(String rawSms, int userId, int sourceId) {
        int smsId = insertRawSms(rawSms);
        if (smsId < 0) return;

        BankSmsParser.SmsResult r = BankSmsParser.parse(rawSms);
        if (!r.success) { markFailed(smsId, "No amount or date"); return; }

        int categoryId = CategoryEngine.getCategoryId(r.merchant);
        TransactionDAO txDao = new TransactionDAO();
        int merchantId = txDao.findOrCreateMerchant(r.merchant);

        Transaction t = new Transaction(userId, merchantId, categoryId, sourceId,
                r.amount, r.date, "INR",
                r.txType.equals("CREDIT") ? "credit" : "parsed", rawSms);
        int txId = txDao.insertTransaction(t);

        updateBankSmsGeneric(smsId, r, txId);
        System.out.println("[SmsDAO] Bank → " + r);
    }

    // ── STEP 1: Store raw SMS immediately ────────────────────────
    private int insertRawSms(String rawSms) {
        try {
            PreparedStatement ps = DBConnection.getConnection()
                    .prepareStatement("INSERT INTO BANK_SMS (raw_sms, parse_status) VALUES (?, 'pending')",
                            Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, rawSms);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            return rs.next() ? rs.getInt(1) : -1;
        } catch (SQLException e) { e.printStackTrace(); return -1; }
    }

    // ── STEP 5A: Update BANK_SMS for Axis format ─────────────────
    private void updateBankSmsAxis(int smsId, AxisSmsParser.SmsResult r, int txId) {
        String sql = "UPDATE BANK_SMS SET currency=?, amount=?, tx_type=?, account_last4=?," +
                " tx_date=?, tx_time=?, upi_type=?, upi_ref=?," +
                " merchant_raw=?, merchant_clean=?, bank_name=?," +
                " transaction_id=?, parse_status='parsed' WHERE sms_id=?";
        try {
            PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql);
            ps.setString(1, r.currency);
            ps.setDouble(2, r.amount);
            ps.setString(3, r.txType);
            ps.setString(4, r.accountLast4);
            ps.setDate  (5, r.txDate != null ? java.sql.Date.valueOf(r.txDate) : null);
            ps.setTime  (6, r.txTime != null ? java.sql.Time.valueOf(r.txTime) : null);
            ps.setString(7, r.upiType);
            ps.setString(8, r.upiRef);
            ps.setString(9, r.merchantRaw);
            ps.setString(10, r.merchantClean);
            ps.setString(11, r.bankName);
            if (txId > 0) ps.setInt(12, txId); else ps.setNull(12, Types.INTEGER);
            ps.setInt   (13, smsId);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ── STEP 5B: Update BANK_SMS for other banks ─────────────────
    private void updateBankSmsGeneric(int smsId, BankSmsParser.SmsResult r, int txId) {
        String sql = "UPDATE BANK_SMS SET currency='INR', amount=?, tx_type=?, account_last4=?," +
                " tx_date=?, upi_ref=?, merchant_clean=?, bank_name=?," +
                " transaction_id=?, parse_status='parsed' WHERE sms_id=?";
        try {
            PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql);
            ps.setDouble(1, r.amount);
            ps.setString(2, r.txType);
            ps.setString(3, r.account4);
            ps.setDate  (4, r.date != null ? java.sql.Date.valueOf(r.date) : null);
            ps.setString(5, r.upiRef);
            ps.setString(6, r.merchant);
            ps.setString(7, r.bankName);
            if (txId > 0) ps.setInt(8, txId); else ps.setNull(8, Types.INTEGER);
            ps.setInt   (9, smsId);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ── Mark failed ───────────────────────────────────────────────
    private void markFailed(int smsId, String reason) {
        try {
            PreparedStatement ps = DBConnection.getConnection()
                    .prepareStatement("UPDATE BANK_SMS SET parse_status='failed', failure_reason=? WHERE sms_id=?");
            ps.setString(1, reason);
            ps.setInt   (2, smsId);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}
