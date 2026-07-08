package db;

import model.Transaction;
import java.sql.*;

// TransactionDAO — Data Access Object for TRANSACTION table operations
// DAO pattern: isolates all database SQL from business/parsing logic

public class TransactionDAO {

    // ── INSERT TRANSACTION ────────────────────────────────────────
    // Inserts one Transaction object into the TRANSACTION table
    // Returns the auto-generated transaction_id
    public int insertTransaction(Transaction t) {
        String sql = "INSERT INTO TRANSACTION " +
                "(user_id, merchant_id, category_id, source_id, " +
                " amount, transaction_date, currency, status, raw_text) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            // RETURN_GENERATED_KEYS → retrieve the auto-incremented transaction_id
            PreparedStatement ps = DBConnection.getConnection()
                    .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt   (1, t.getUserId());
            ps.setInt   (2, t.getMerchantId());
            ps.setInt   (3, t.getCategoryId());
            ps.setInt   (4, t.getSourceId());
            ps.setDouble(5, t.getAmount());
            // Convert String date "2024-10-15" → java.sql.Date for MySQL DATE column
            ps.setDate  (6, java.sql.Date.valueOf(t.getTransactionDate()));
            ps.setString(7, t.getCurrency());
            ps.setString(8, t.getStatus());
            ps.setString(9, t.getRawText());
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                System.out.println("  Inserted transaction_id: " + id);
                return id;
            }
        } catch (SQLException e) {
            System.err.println("ERROR inserting transaction: " + e.getMessage());
        }
        return -1;
    }

    // ── FIND OR CREATE MERCHANT ───────────────────────────────────
    // Looks up merchant by name (case-insensitive)
    // If not found → inserts new row in MERCHANT table
    // Returns merchant_id (existing or newly created)
    public int findOrCreateMerchant(String merchantName) {
        try {
            // STEP 1: Try to find existing merchant
            PreparedStatement find = DBConnection.getConnection()
                    .prepareStatement("SELECT merchant_id FROM MERCHANT WHERE LOWER(merchant_name) = LOWER(?)");
            find.setString(1, merchantName);
            ResultSet rs = find.executeQuery();
            if (rs.next()) return rs.getInt("merchant_id");  // found → return existing id

            // STEP 2: Not found → insert new merchant
            PreparedStatement ins = DBConnection.getConnection()
                    .prepareStatement("INSERT INTO MERCHANT (merchant_name) VALUES (?)",
                            Statement.RETURN_GENERATED_KEYS);
            ins.setString(1, merchantName);
            ins.executeUpdate();
            ResultSet gen = ins.getGeneratedKeys();
            if (gen.next()) {
                System.out.println("  New merchant: " + merchantName);
                return gen.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("ERROR in findOrCreateMerchant: " + e.getMessage());
        }
        return 0;  // 0 = unknown if error
    }

    // ── INSERT PARSED LOG ─────────────────────────────────────────
    // Records what pattern matched (or failed) — audit trail
    public void insertParsedLog(int transactionId, String originalText,
                                String matchedPattern, boolean success) {
        String sql = "INSERT INTO PARSED_LOG " +
                "(transaction_id, original_text, matched_pattern, success) VALUES (?,?,?,?)";
        try {
            PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql);
            if (transactionId > 0) ps.setInt(1, transactionId);
            else                   ps.setNull(1, Types.INTEGER);
            ps.setString (2, originalText);
            ps.setString (3, matchedPattern);
            ps.setBoolean(4, success);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("ERROR inserting log: " + e.getMessage());
        }
    }
}
