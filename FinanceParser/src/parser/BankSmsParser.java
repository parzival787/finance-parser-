package parser;
import java.util.regex.*;
// BankSmsParser — handles all Indian banks' single-line SMS
public class BankSmsParser {
    public static class SmsResult {
        public String bankName, txType, date, account4, merchant, upiRef;
        public double amount, balanceAfter;
        public boolean success;
        public String rawSms;

        @Override
        public String toString() {
            if (!success) return "[FAILED] no amount/date";
            return String.format("[%s] %s Rs.%.2f Date:%s Merchant:%s Bal:%.2f", bankName, txType, amount, date, merchant, balanceAfter);
        }
    }

    public static String detectBank(String s) {
        String u = s.toUpperCase();
        if (u.contains("SBI")) return "SBI";
        if (u.contains("HDFC")) return "HDFC";
        if (u.contains("ICICI")) return "ICICI";
        if (u.contains("AXIS")) return "Axis";
        if (u.contains("KOTAK")) return "Kotak";
        if (u.contains("PNB")) return "PNB";
        if (u.contains("BOB") || u.contains("BANK OF BARODA")) return "BOB";
        if (u.contains("CANARA")) return "Canara";
        if (u.contains("PAYTM")) return "Paytm";
        if (u.contains("PHONEPE")) return "PhonePe";
        return "Unknown";
    }

    public static String detectTxType(String s) {
        String u = s.toUpperCase();
        if (u.contains("CREDITED") || u.contains("RECEIVED") || u.contains("DEPOSITED")) return "CREDIT";
        if (u.contains("DEBITED") || u.contains("CHARGED") || u.contains("WITHDRAWN") || u.contains("PURCHASE"))
            return "DEBIT";
        return "UNKNOWN";
    }

    private static final Pattern AMT = Pattern.compile("(?:Rs\\.?|INR|₹)\\s*([\\d,]+\\.?\\d*)", Pattern.CASE_INSENSITIVE);

    public static double extractAmount(String s) {
        Matcher m = AMT.matcher(s);
        return m.find() ? Double.parseDouble(m.group(1).replace(",", "")) : 0.0;
    }

    private static final Pattern ACCT = Pattern.compile("(?:\\*\\*|XX|x{2,})(\\d{4})", Pattern.CASE_INSENSITIVE);

    public static String extractAccount4(String s) {
        Matcher m = ACCT.matcher(s);
        return m.find() ? m.group(1) : null;
    }

    private static final Pattern BAL = Pattern.compile(
            "(?:Avl\\.?\\s*[Bb]al|[Bb]alance|Bal)\\s*[:\\-]?\\s*(?:Rs\\.?|INR|₹)?\\s*([\\d,]+\\.?\\d*)", Pattern.CASE_INSENSITIVE);

    public static double extractBalance(String s) {
        Matcher m = BAL.matcher(s);
        return m.find() ? Double.parseDouble(m.group(1).replace(",", "")) : 0.0;
    }

    private static final Pattern UPI_R = Pattern.compile("(?:UPI[:/]|VPA\\s+)(\\S+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern REF_N = Pattern.compile("(?:Ref\\.?\\s*No\\.?|RefNo)\\s*([\\dA-Z]+)", Pattern.CASE_INSENSITIVE);

    public static String extractUpiRef(String s) {
        Matcher m = UPI_R.matcher(s);
        if (m.find()) return m.group(1).trim();
        m = REF_N.matcher(s);
        return m.find() ? m.group(1).trim() : null;
    }

    private static final Pattern M_UPI = Pattern.compile("UPI/([A-Za-z0-9]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern M_INFO = Pattern.compile("Info:\\s*([A-Za-z0-9 &@._\\-]+?)(?:\\s|$)", Pattern.CASE_INSENSITIVE);
    private static final Pattern M_BY = Pattern.compile("(?:\\bby\\b|\\bat\\b)\\s+([A-Za-z0-9 &@._\\-]+?)(?:\\.|,|\\s*Avl|\\s*Bal|$)", Pattern.CASE_INSENSITIVE);

    public static String extractMerchant(String s) {
        Matcher m = M_UPI.matcher(s);
        if (m.find()) return m.group(1).trim();
        m = M_INFO.matcher(s);
        if (m.find()) return m.group(1).trim();
        m = M_BY.matcher(s);
        if (m.find()) return m.group(1).trim();
        return "Unknown";
    }

    private static final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
    private static final Pattern DT = Pattern.compile("(\\d{1,2})[\\- ](Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*[\\- ](\\d{2,4})", Pattern.CASE_INSENSITIVE);
    private static final Pattern DN = Pattern.compile("(\\d{1,2})[\\-/](\\d{1,2})[\\-/](\\d{2,4})");

    public static String extractDate(String s) {
        Matcher m = DT.matcher(s);
        if (m.find()) {
            String d = String.format("%02d", Integer.parseInt(m.group(1)));
            String y = m.group(3).length() == 2 ? "20" + m.group(3) : m.group(3);
            String mo = "01";
            for (int i = 0; i < MONTHS.length; i++)
                if (m.group(2).equalsIgnoreCase(MONTHS[i])) {
                    mo = String.format("%02d", i + 1);
                    break;
                }
            return y + "-" + mo + "-" + d;
        }
        m = DN.matcher(s);
        if (m.find()) {
            String p1 = m.group(1), p2 = m.group(2), p3 = m.group(3);
            if (p3.length() == 2) p3 = "20" + p3;
            return p3 + "-" + String.format("%02d", Integer.parseInt(p2)) + "-" + String.format("%02d", Integer.parseInt(p1));
        }
        return null;
    }

    public static SmsResult parse(String rawSms) {
        SmsResult r = new SmsResult();
        r.rawSms = rawSms;
        r.bankName = detectBank(rawSms);
        r.txType = detectTxType(rawSms);
        r.amount = extractAmount(rawSms);
        r.date = extractDate(rawSms);
        r.account4 = extractAccount4(rawSms);
        r.merchant = extractMerchant(rawSms);
        r.upiRef = extractUpiRef(rawSms);
        r.balanceAfter = extractBalance(rawSms);
        r.success = (r.amount > 0 && r.date != null);
        return r;
    }

    public static void main(String[] args) {
        String[] tests = {
                "Rs.499.00 debited from ac **6789 on 15-10-24. UPI:123456789012. Avl bal:Rs.12,340.50-SBI",
                "Rs 2399 debited from HDFC Bank A/c **1234 on 18/10/24 by AMAZON. Avl Bal: Rs 8,901.00",
                "ICICI Bank Acct XX5678 debited for Rs 649.00 on 16-Oct-24. Info: NETFLIX VPA xyz@icici",
        };
        for (String s : tests) System.out.println(parse(s));


    }
}