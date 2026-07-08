import parser.AxisSmsParser;
import parser.BankSmsParser;
import parser.SmsFileReader;
import parser.CategoryEngine;
import db.DBConnection;
import db.SmsDAO;
import report.QueryRunner;
import report.ChartDataExporter;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class Main {

    // ── USER AND SOURCE IDs (must exist in MySQL USER and INPUT_SOURCE tables) ──
    private static final int DEFAULT_USER_ID   = 1;
    private static final int DEFAULT_SOURCE_ID = 1;

    public static void main(String[] args) {

        System.out.println("=".repeat(50));
        System.out.println("  Finance Parser — Starting Up");
        System.out.println("=".repeat(50));

        // ── STEP 1: READ SMS FROM FILE ───────────────────────────────────────
        // Use the path shown in your IntelliJ project panel.
        // Right-click sms_inbox.txt → Copy Path/Reference → Absolute Path → paste here
        // Make sure the filename ends with .txt
        String smsFilePath = "C:\\Users\\Administrator\\Desktop\\FinanceParser\\FinanceParser\\resources\\sms_inbox";
        // If relative path fails, use absolute:
        // String smsFilePath = "C:\\Users\\Administrator\\Desktop\\FinanceParser\\FinanceParser\\resources\\sms_inbox.txt";

        List<String> smsList = SmsFileReader.readAllSms(smsFilePath);

        if (smsList.isEmpty()) {
            System.out.println("⚠  No SMS messages found. Check that:");
            System.out.println("   1. sms_inbox.txt exists in the resources/ folder");
            System.out.println("   2. The file has content (not empty)");
            System.out.println("   3. Messages are separated by blank lines");
            System.out.println("   4. File is named sms_inbox.txt (not sms_inbox)");
        }

        // ── STEP 2: PROCESS EACH SMS ─────────────────────────────────────────
        SmsDAO smsDao = new SmsDAO();
        int success = 0, failed = 0;

        for (String sms : smsList) {
            // Show first line only for cleaner output
            String firstLine = sms.split("\n")[0];
            System.out.println("\nProcessing: " + firstLine);

            boolean isAxis = sms.contains("\n") &&
                    sms.matches("(?s)(?i)INR\\s+[\\d,]+\\.?\\d*\\s+(debited|credited).*");

            if (isAxis) {
                AxisSmsParser.SmsResult r = AxisSmsParser.parse(sms);
                if (!r.success) { System.out.println("  [FAILED] " + r.failureReason); failed++; continue; }

                // DUPLICATE CHECK
                if (isDuplicateUpiRef(r.upiRef)) {
                    System.out.println("  [SKIPPED] Duplicate UPI ref: " + r.upiRef);
                    continue;
                }
                smsDao.ingestAxisSms(sms, DEFAULT_USER_ID, DEFAULT_SOURCE_ID);
                System.out.println("  [OK] " + r.amount + " | " + r.merchantClean + " | " + r.txDate);
                success++;
            } else {
                BankSmsParser.SmsResult r = BankSmsParser.parse(sms);
                if (!r.success) { System.out.println("  [FAILED] no amount/date"); failed++; continue; }

                if (isDuplicateUpiRef(r.upiRef)) {
                    System.out.println("  [SKIPPED] Duplicate: " + r.upiRef);
                    continue;
                }
                smsDao.ingestBankSms(sms, DEFAULT_USER_ID, DEFAULT_SOURCE_ID);
                System.out.println("  [OK] Rs." + r.amount + " | " + r.merchant + " | " + r.date);
                success++;
            }

        }



        // ── STEP 3: SUMMARY ──────────────────────────────────────────────────
        System.out.println("\n" + "=".repeat(50));
        System.out.println("  Parsing Complete");
        System.out.printf ("  Success: %d  |  Failed: %d%n", success, failed);
        System.out.println("=".repeat(50));

        // ── STEP 4: SQL ANALYTICS REPORTS ────────────────────────────────────
        QueryRunner.runAllReports(DEFAULT_USER_ID);

        // ── STEP 5: WRITE HTML CHART DASHBOARD ───────────────────────────────
        System.out.println("\n=== Generating Chart Dashboard ===");
        ChartDataExporter.exportAll(DEFAULT_USER_ID, "2024-10");
        ChartDataExporter.writeHtmlDashboard(DEFAULT_USER_ID, "C:\\Users\\Administrator\\Desktop\\FinanceParser\\charts\\index.html");

        // ── STEP 6: CLOSE DB ─────────────────────────────────────────────────
        DBConnection.close();
        System.out.println("\nDone! Open charts/index.html in your browser to see charts.");
    }

    /**
     * Detects whether an SMS is in Axis Bank's multi-line format.
     *
     * Axis Bank multi-line SMS always has:
     *   Line 1: "INR <amount> debited"  OR  "INR <amount> credited"
     *           The line starts with INR, has a number, then debited/credited
     *           Nothing else on that line
     *
     * Single-line bank SMS (SBI, HDFC, ICICI etc.) will have
     *   "Rs." or "Rs " or "debited from" all on one long line.
     */
    private static boolean isAxisMultiLine(String sms) {
        if (!sms.contains("\n")) return false; // must have multiple lines

        // Get only the first line
        String firstLine = sms.split("\n")[0].trim();

        // Axis first line pattern: "INR 72.00 debited" or "INR 499.00 credited"
        // Must start with INR, then a number, then debited or credited
        return firstLine.matches("(?i)INR\\s+[\\d,]+\\.?\\d*\s+(debited|credited).*");
    }

    private static boolean isDuplicateUpiRef(String upiRef) {
        if (upiRef == null || upiRef.trim().isEmpty()) return false;
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(
                "SELECT COUNT(*) FROM BANK_SMS WHERE upi_ref = ?")) {
            ps.setString(1, upiRef);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("Duplicate check error: " + e.getMessage());
        }
        return false;
    }

}
