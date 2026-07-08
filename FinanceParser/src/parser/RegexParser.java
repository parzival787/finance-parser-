package parser;
import java.util.regex.*;

public class RegexParser {

    // Fixed backslashes (\\d instead of \d) and escaped the dot in Rs.
    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
            "(?:Rs\\.?|₹|\\$)\\s*([\\d,]+\\.?[\\d]*)", Pattern.CASE_INSENSITIVE);

    public static double extractAmount(String text) {
        Matcher m = AMOUNT_PATTERN.matcher(text);
        if (m.find()) return Double.parseDouble(m.group(1).replace(",", ""));
        return 0.0;
    }

    private static final Pattern DATE_ISO  = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");
    private static final Pattern DATE_SLASH = Pattern.compile("(\\d{1,2}/\\d{1,2}/\\d{4})");
    private static final Pattern DATE_TEXT = Pattern.compile(
            "(\\d{1,2}[-\\s](?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*[-\\s]\\d{4})",
            Pattern.CASE_INSENSITIVE);

    public static String extractDate(String text) {
        Matcher m = DATE_ISO.matcher(text);
        if (m.find()) return m.group(1);

        m = DATE_SLASH.matcher(text);
        if (m.find()) {
            String[] p = m.group(1).split("/");
            // Assuming MM/DD/YYYY based on your original logic
            return p[2] + "-" + String.format("%02d", Integer.parseInt(p[0])) + "-" + String.format("%02d", Integer.parseInt(p[1]));
        }

        m = DATE_TEXT.matcher(text);
        if (m.find()) return convertTextDate(m.group(1));
        return null;
    }

    private static final String[] MONTHS = {"jan","feb","mar","apr","may","jun","jul","aug","sep","oct","nov","dec"};

    private static String convertTextDate(String raw) {
        // Fixed backslashes in regex
        String clean = raw.replaceAll("[-\\s]", " ").trim();
        String[] p = clean.split("\\s+");
        if (p.length < 3) return null;

        String day = String.format("%02d", Integer.parseInt(p[0]));
        String yr = p[2].length() == 2 ? "20" + p[2] : p[2];
        String mon = "01";
        for (int i = 0; i < MONTHS.length; i++) {
            if (p[1].toLowerCase().startsWith(MONTHS[i])) {
                mon = String.format("%02d", i + 1);
                break;
            }
        }
        return yr + "-" + mon + "-" + day;
    }

    // Added double backslashes for word boundaries
    private static final Pattern MERCHANT_AT = Pattern.compile(
            "(?<=\\bat\\b)\\s*([A-Za-z0-9 &\\-]+?)(?:\\s+on|\\s+dated|\\.|,|$)", Pattern.CASE_INSENSITIVE);
    private static final Pattern MERCHANT_TO = Pattern.compile(
            "(?<=\\bto\\b)\\s*([A-Za-z0-9 &\\-]+?)(?:\\.|,|\\s+on|$)", Pattern.CASE_INSENSITIVE);

    public static String extractMerchant(String text) {
        Matcher m = MERCHANT_AT.matcher(text);
        if (m.find()) return m.group(1).trim();
        m = MERCHANT_TO.matcher(text);
        if (m.find()) return m.group(1).trim();
        return "Unknown";
    }

    private static final Pattern CURRENCY_PATTERN = Pattern.compile(
            "\\b(INR|USD|EUR|GBP|JPY)\\b|(?:Rs\\.?|₹)", Pattern.CASE_INSENSITIVE);

    public static String extractCurrency(String text) {
        Matcher m = CURRENCY_PATTERN.matcher(text);
        if (m.find()) {
            String f = m.group().toUpperCase().trim();
            return (f.startsWith("RS") || f.equals("₹")) ? "INR" : f;
        }
        return "INR";
    }

    public static void main(String[] args) {
        String[] tests = {
                "Your card was charged Rs.499 at Swiggy on 2024-10-15.",
                "Payment of Rs.649 to Netflix. Date: 10/16/2024.",
                "Amazon purchase Rs.2399 on 18-Oct-2024.",
        };
        for (String t : tests) {
            System.out.println("Input:    " + t);
            System.out.println("Amount:   " + extractAmount(t));
            System.out.println("Date:     " + extractDate(t));
            System.out.println("Merchant: " + extractMerchant(t));
            System.out.println("Currency: " + extractCurrency(t));
            System.out.println("---");
        }
    }
}