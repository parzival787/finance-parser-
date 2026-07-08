package parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;

/**
 * AxisSmsParser — parses Axis Bank's real multi-line SMS format.
 *
 * REAL SMS FORMAT (from your phone):
 *   Line 1:  INR 72.00 debited
 *   Line 2:  A/c no. XX8814
 *   Line 3:  07-04-26, 21:27:59
 *   Line 4:  UPI/P2M/646304856626/SAI BHAKRI CENTRE
 *   Line 5:  Not you? SMS BLOCKUPI Cust ID to 919951860002
 *   Line 6:  Axis Bank
 */
public class AxisSmsParser {

    // ── RESULT HOLDER ─────────────────────────────────────────────────────
    public static class SmsResult {
        public String currency    = "INR";
        public double amount      = 0.0;
        public String txType      = "DEBIT";
        public String accountLast4;
        public String txDate;       // ISO: 2026-04-07
        public String txTime;       // 21:27:59
        public String txDateTime;   // 2026-04-07 21:27:59
        public String upiType;      // P2M, P2P, NEFT etc.
        public String upiRef;       // 646304856626
        public String merchantRaw;  // SAI BHAKRI CENTRE
        public String merchantClean;// Sai Bhakri Centre
        public String bankName = "Axis Bank";
        public boolean success = false;
        public String failureReason;
        public String rawSms;

        @Override
        public String toString() {
            if (!success) return "[FAILED] " + failureReason;
            return String.format("[%s|%s] %s %.2f | Acct:**%s | %s %s | %s/%s | Merchant: %s",
                    bankName, txType, currency, amount, accountLast4,
                    txDate, txTime, upiType, upiRef, merchantClean);
        }
    }

    // ── MAIN PARSE METHOD ─────────────────────────────────────────────────
    public static SmsResult parse(String rawSms) {
        SmsResult r = new SmsResult();
        r.rawSms = rawSms;

        // Split into lines, skip empty ones
        List<String> lines = new ArrayList<>();
        for (String line : rawSms.split("\n")) {
            String t = line.trim();
            if (!t.isEmpty()) lines.add(t);
        }

        System.out.println("  Lines found: " + lines.size());
        for (int i = 0; i < lines.size(); i++) {
            System.out.println("  Line " + i + ": [" + lines.get(i) + "]");
        }

        if (lines.size() < 3) {
            r.failureReason = "Too few lines: " + lines.size() + " (need at least 3)";
            return r;
        }

        // Process each line
        parseLine1_AmountAndType(r, lines.get(0));
        if (lines.size() > 1) parseLine2_Account(r, lines.get(1));
        if (lines.size() > 2) parseLine3_DateTime(r, lines.get(2));
        if (lines.size() > 3) parseLine4_UpiInfo(r, lines.get(3));
        parseLastLine_BankName(r, lines);

        // Validate
        if (r.amount <= 0) {
            r.failureReason = "No amount found in line 1: [" + lines.get(0) + "]";
            return r;
        }
        if (r.txDate == null) {
            r.failureReason = "No date found in line 3: [" + (lines.size() > 2 ? lines.get(2) : "N/A") + "]";
            return r;
        }

        r.success = true;
        return r;
    }

    // ── LINE 1: "INR 72.00 debited" ───────────────────────────────────────
    // Handles: INR 72.00 debited / INR 2,399.00 credited / INR 500 debited
    private static final Pattern LINE1 = Pattern.compile(
            "^(INR|USD|EUR|GBP)\\s+([\\d,]+\\.?\\d*)\\s+(debited|credited)",
            Pattern.CASE_INSENSITIVE
    );

    private static void parseLine1_AmountAndType(SmsResult r, String line) {
        Matcher m = LINE1.matcher(line.trim());
        if (m.find()) {
            r.currency = m.group(1).toUpperCase();
            // Remove commas: "2,399.00" → "2399.00"
            String amtStr = m.group(2).replace(",", "");
            r.amount  = Double.parseDouble(amtStr);
            r.txType  = m.group(3).equalsIgnoreCase("debited") ? "DEBIT" : "CREDIT";
        } else {
            // Fallback: try to find any number after INR
            Matcher fb = Pattern.compile("([\\d,]+\\.?\\d{0,2})").matcher(line);
            if (fb.find()) {
                r.amount   = Double.parseDouble(fb.group(1).replace(",", ""));
                r.currency = "INR";
                r.txType   = line.toLowerCase().contains("credit") ? "CREDIT" : "DEBIT";
            }
        }
    }

    // ── LINE 2: "A/c no. XX8814" ──────────────────────────────────────────
    // Handles: A/c no. XX8814 / Ac **1234 / A/C No: XX5678
    private static final Pattern LINE2 = Pattern.compile(
            "[Aa]/?[Cc]\\s*[Nn][Oo]\\.?\\s*[Xx*]{1,}(\\d{4})"
    );

    private static void parseLine2_Account(SmsResult r, String line) {
        Matcher m = LINE2.matcher(line);
        if (m.find()) {
            r.accountLast4 = m.group(1);
        } else {
            // Fallback: grab last 4 digits after XX or **
            Matcher fb = Pattern.compile("[Xx*]{1,}(\\d{4})").matcher(line);
            if (fb.find()) r.accountLast4 = fb.group(1);
        }
    }

    // ── LINE 3: "07-04-26, 21:27:59" ─────────────────────────────────────
    // Format: DD-MM-YY, HH:mm:ss  (year is 2 digits — prepend "20")
    // Also handles: DD-MM-YYYY or DD/MM/YY formats
    private static final Pattern LINE3_FULL = Pattern.compile(
            "(\\d{1,2})[\\-/](\\d{1,2})[\\-/](\\d{2,4})[,\\s]+(\\d{2}):(\\d{2}):(\\d{2})"
    );
    private static final Pattern LINE3_DATE_ONLY = Pattern.compile(
            "(\\d{1,2})[\\-/](\\d{1,2})[\\-/](\\d{2,4})"
    );

    private static void parseLine3_DateTime(SmsResult r, String line) {
        Matcher m = LINE3_FULL.matcher(line);
        if (m.find()) {
            String day = String.format("%02d", Integer.parseInt(m.group(1)));
            String mon = String.format("%02d", Integer.parseInt(m.group(2)));
            String yr  = m.group(3).length() == 2 ? "20" + m.group(3) : m.group(3);
            r.txDate     = yr + "-" + mon + "-" + day;
            r.txTime     = m.group(4) + ":" + m.group(5) + ":" + m.group(6);
            r.txDateTime = r.txDate + " " + r.txTime;
        } else {
            // Date only fallback
            Matcher fb = LINE3_DATE_ONLY.matcher(line);
            if (fb.find()) {
                String day = String.format("%02d", Integer.parseInt(fb.group(1)));
                String mon = String.format("%02d", Integer.parseInt(fb.group(2)));
                String yr  = fb.group(3).length() == 2 ? "20" + fb.group(3) : fb.group(3);
                r.txDate = yr + "-" + mon + "-" + day;
            }
        }
    }

    // ── LINE 4: "UPI/P2M/646304856626/SAI BHAKRI CENTRE" ─────────────────
    // Split on "/" — parts: [0]=UPI [1]=type [2]=ref [3+]=merchant
    private static void parseLine4_UpiInfo(SmsResult r, String line) {
        String trimmed = line.trim();

        if (trimmed.toUpperCase().startsWith("UPI")) {
            // UPI format: UPI/P2M/ref/MERCHANT NAME
            String[] parts = trimmed.split("/");
            if (parts.length >= 2) r.upiType = parts[1].trim();
            if (parts.length >= 3) r.upiRef  = parts[2].trim();
            if (parts.length >= 4) {
                // Join remaining parts as merchant (handles merchant names with /)
                StringBuilder sb = new StringBuilder();
                for (int i = 3; i < parts.length; i++) {
                    if (i > 3) sb.append("/");
                    sb.append(parts[i].trim());
                }
                r.merchantRaw   = sb.toString().trim();
                r.merchantClean = toTitleCase(r.merchantRaw);
            }
        } else if (trimmed.toUpperCase().matches("(NEFT|IMPS|RTGS)/.*")) {
            // NEFT/IMPS format: NEFT/ref/SENDER NAME
            String[] parts = trimmed.split("/", 3);
            r.upiType = parts[0].toUpperCase();
            if (parts.length >= 2) r.upiRef = parts[1].trim();
            if (parts.length >= 3) {
                r.merchantRaw   = parts[2].trim();
                r.merchantClean = toTitleCase(r.merchantRaw);
            }
        } else {
            // Unknown format — store whole line as merchant
            r.merchantRaw   = trimmed;
            r.merchantClean = toTitleCase(trimmed);
            r.upiType = "UNKNOWN";
        }

        // Default upiType if not set
        if (r.upiType == null) r.upiType = "UPI";
        // Default merchant if not found
        if (r.merchantRaw == null || r.merchantRaw.isEmpty()) {
            r.merchantRaw   = "Unknown";
            r.merchantClean = "Unknown";
        }
    }

    // ── LAST LINE: Bank Name (skip warning lines) ─────────────────────────
    private static void parseLastLine_BankName(SmsResult r, List<String> lines) {
        for (int i = lines.size() - 1; i >= 0; i--) {
            String line = lines.get(i).trim().toLowerCase();
            if (line.startsWith("not you")   ||
                    line.startsWith("sms block") ||
                    line.startsWith("call ")     ||
                    line.startsWith("to dispute")||
                    line.startsWith("if not you")) continue;
            r.bankName = lines.get(i).trim();
            break;
        }
        if (r.bankName == null || r.bankName.isEmpty()) r.bankName = "Axis Bank";
    }

    // ── TITLE CASE: "SAI BHAKRI CENTRE" → "Sai Bhakri Centre" ───────────
    private static String toTitleCase(String input) {
        if (input == null || input.isEmpty()) return input;
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = true;
        for (char c : input.toCharArray()) {
            if (Character.isWhitespace(c) || c == '/') { sb.append(c); nextUpper = true; }
            else if (nextUpper) { sb.append(Character.toUpperCase(c)); nextUpper = false; }
            else { sb.append(Character.toLowerCase(c)); }
        }
        return sb.toString();
    }

    // ── QUICK TEST ────────────────────────────────────────────────────────
    public static void main(String[] args) {
        // Your EXACT real SMS from the screenshot
        String realSms =
                "INR 72.00 debited\n" +
                        "A/c no. XX8814\n" +
                        "07-04-26, 21:27:59\n" +
                        "UPI/P2M/646304856626/SAI BHAKRI CENTRE\n" +
                        "Not you? SMS BLOCKUPI Cust ID to 919951860002\n" +
                        "Axis Bank";

        String swiggy =
                "INR 499.00 debited\n" +
                        "A/c no. XX8814\n" +
                        "10-04-26, 19:22:05\n" +
                        "UPI/P2M/223344556677/SWIGGY\n" +
                        "Not you? SMS BLOCKUPI Cust ID to 919951860002\n" +
                        "Axis Bank";

        String credited =
                "INR 500.00 credited\n" +
                        "A/c no. XX8814\n" +
                        "09-04-26, 13:10:44\n" +
                        "UPI/P2P/998877665544/RAHUL SHARMA\n" +
                        "Not you? SMS BLOCKUPI Cust ID to 919951860002\n" +
                        "Axis Bank";

        String largeAmt =
                "INR 2,399.00 debited\n" +
                        "A/c no. XX8814\n" +
                        "08-04-26, 22:05:11\n" +
                        "UPI/P2M/555444333222/AMAZON SELLER SERVI\n" +
                        "Not you? SMS BLOCKUPI Cust ID to 919951860002\n" +
                        "Axis Bank";

        System.out.println("=".repeat(60));
        System.out.println("AxisSmsParser — Test Results");
        System.out.println("=".repeat(60));

        for (String sms : new String[]{realSms, swiggy, credited, largeAmt}) {
            SmsResult result = parse(sms);
            System.out.println("\nResult: " + result);
            System.out.println("-".repeat(40));
        }
    }
}
