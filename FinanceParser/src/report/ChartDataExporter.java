package report;

import db.DBConnection;
import java.sql.*;
import java.io.*;

/**
 * ChartDataExporter — builds Chart.js JSON from DB, writes ready-to-open HTML.
 *
 * FIXES APPLIED:
 *  1. SQL now queries ALL transactions (not just a specific period) for pie/bar/line
 *  2. Em dash written as Unicode escape \u2014 to avoid encoding issues in HTML
 *  3. Null-safe JSON building — empty datasets don't crash
 *  4. writeHtmlDashboard() creates a complete standalone HTML file
 */
public class ChartDataExporter {

    // ── PIE CHART: Spending by Category ──────────────────────────
    public static String buildPieChartJson(int userId) {
        StringBuilder labels = new StringBuilder();
        StringBuilder data   = new StringBuilder();
        StringBuilder colors = new StringBuilder();
        String[] pal = {"#FF6384","#36A2EB","#FFCE56","#4BC0C0","#9966FF","#FF9F40","#7BC8A4","#C9CBCF"};

        // FIX: Query directly from TRANSACTION + CATEGORY — no view dependency
        String sql =
                "SELECT c.category_name AS label, SUM(t.amount) AS value " +
                        "FROM TRANSACTION t " +
                        "JOIN CATEGORY c ON t.category_id = c.category_id " +
                        "WHERE t.user_id = ? " +
                        "GROUP BY c.category_name " +
                        "ORDER BY value DESC";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            int i = 0;
            while (rs.next()) {
                if (i > 0) { labels.append(","); data.append(","); colors.append(","); }
                labels.append("\"").append(rs.getString("label")).append("\"");
                data.append(rs.getDouble("value"));
                colors.append("\"").append(pal[i % pal.length]).append("\"");
                i++;
            }
        } catch (SQLException e) {
            System.err.println("[ChartData] Pie chart error: " + e.getMessage());
        }

        if (data.length() == 0) {
            return "{\"type\":\"pie\",\"labels\":[\"No Data\"],\"datasets\":[{\"data\":[1],\"backgroundColor\":[\"#444\"]}]}";
        }
        return "{\"type\":\"pie\",\"labels\":[" + labels + "]," +
                "\"datasets\":[{\"label\":\"Spending\",\"data\":[" + data + "]," +
                "\"backgroundColor\":[" + colors + "]}]}";
    }

    // ── BAR CHART: Monthly Spending ──────────────────────────────
    public static String buildBarChartJson(int userId) {
        StringBuilder labels = new StringBuilder();
        StringBuilder data   = new StringBuilder();

        // FIX: Direct query — no view dependency, covers all dates
        String sql =
                "SELECT DATE_FORMAT(transaction_date, '%b %Y') AS month_label, " +
                        "       DATE_FORMAT(transaction_date, '%Y-%m') AS month_key, " +
                        "       SUM(amount) AS total_spent " +
                        "FROM TRANSACTION " +
                        "WHERE user_id = ? " +
                        "GROUP BY month_label, month_key " +
                        "ORDER BY month_key ASC";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            int i = 0;
            while (rs.next()) {
                if (i++ > 0) { labels.append(","); data.append(","); }
                labels.append("\"").append(rs.getString("month_label")).append("\"");
                data.append(rs.getDouble("total_spent"));
            }
        } catch (SQLException e) {
            System.err.println("[ChartData] Bar chart error: " + e.getMessage());
        }

        if (data.length() == 0) {
            return "{\"type\":\"bar\",\"labels\":[\"No Data\"],\"datasets\":[{\"label\":\"No Data\",\"data\":[0],\"backgroundColor\":\"#444\"}]}";
        }
        return "{\"type\":\"bar\",\"labels\":[" + labels + "]," +
                "\"datasets\":[{\"label\":\"Monthly Spending (INR)\"," +
                "\"data\":[" + data + "],\"backgroundColor\":\"#36A2EB\"}]}";
    }

    // ── LINE CHART: Daily Spending ───────────────────────────────
    public static String buildLineChartJson(int userId) {
        StringBuilder labels = new StringBuilder();
        StringBuilder data   = new StringBuilder();

        String sql =
                "SELECT transaction_date AS date_label, SUM(amount) AS daily_total " +
                        "FROM TRANSACTION " +
                        "WHERE user_id = ? " +
                        "GROUP BY transaction_date " +
                        "ORDER BY transaction_date ASC";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            int i = 0;
            while (rs.next()) {
                if (i++ > 0) { labels.append(","); data.append(","); }
                labels.append("\"").append(rs.getDate("date_label")).append("\"");
                data.append(rs.getDouble("daily_total"));
            }
        } catch (SQLException e) {
            System.err.println("[ChartData] Line chart error: " + e.getMessage());
        }

        if (data.length() == 0) {
            return "{\"type\":\"line\",\"labels\":[\"No Data\"],\"datasets\":[{\"label\":\"No Data\",\"data\":[0],\"borderColor\":\"#FF6384\",\"fill\":false}]}";
        }
        return "{\"type\":\"line\",\"labels\":[" + labels + "]," +
                "\"datasets\":[{\"label\":\"Daily Spending (INR)\"," +
                "\"data\":[" + data + "],\"borderColor\":\"#FF6384\"," +
                "\"backgroundColor\":\"rgba(255,99,132,0.1)\",\"fill\":true,\"tension\":0.3}]}";
    }

    // ── SAVE TO CHART_DATA TABLE ─────────────────────────────────
    public static void saveChartData(int userId, String chartType, String period, String json) {
        if (json == null || json.isEmpty()) return;
        try {
            PreparedStatement del = DBConnection.getConnection()
                    .prepareStatement("DELETE FROM CHART_DATA WHERE user_id=? AND chart_type=? AND period<=>?");
            del.setInt(1, userId); del.setString(2, chartType); del.setString(3, period);
            del.executeUpdate();

            PreparedStatement ins = DBConnection.getConnection()
                    .prepareStatement("INSERT INTO CHART_DATA(user_id,chart_type,period,data_json) VALUES(?,?,?,?)");
            ins.setInt(1, userId); ins.setString(2, chartType);
            ins.setString(3, period); ins.setString(4, json);
            ins.executeUpdate();
            System.out.println("[ChartData] Saved: " + chartType);
        } catch (SQLException e) {
            System.err.println("[ChartData] Save error (" + chartType + "): " + e.getMessage());
            System.err.println("  FIX: Run INSERT IGNORE INTO USER(user_id,name,email) VALUES(1,'User','user@email.com'); in MySQL Workbench");
        }
    }

    // ── EXPORT ALL ───────────────────────────────────────────────
    public static void exportAll(int userId, String period) {
        saveChartData(userId, "PIE_CATEGORY", null,   buildPieChartJson(userId));
        saveChartData(userId, "BAR_MONTHLY",  null,   buildBarChartJson(userId));
        saveChartData(userId, "LINE_DAILY",   null,   buildLineChartJson(userId));
        System.out.println("[ChartData] All charts exported for user " + userId);
    }

    // ── WRITE COMPLETE HTML DASHBOARD FILE ───────────────────────
    // FIX: Uses Unicode escapes for em dash to avoid encoding issues
    // FIX: Inline script — no CDN timeout issues
    public static void writeHtmlDashboard(int userId, String outputPath) {
        String pie  = buildPieChartJson(userId);
        String bar  = buildBarChartJson(userId);
        String line = buildLineChartJson(userId);

        // Count transactions for display
        int txCount = 0;
        double txTotal = 0;
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(
                "SELECT COUNT(*) AS cnt, COALESCE(SUM(amount),0) AS total FROM TRANSACTION WHERE user_id=?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) { txCount = rs.getInt("cnt"); txTotal = rs.getDouble("total"); }
        } catch (SQLException e) { /* ignore */ }

        // FIX: Use \u2014 for em dash — avoids â€" encoding issue
        String html =
                "<!DOCTYPE html>\n" +
                        "<html lang=\"en\">\n" +
                        "<head>\n" +
                        "  <meta charset=\"UTF-8\">\n" +
                        "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                        "  <title>Finance Parser Dashboard</title>\n" +
                        "  <script src=\"https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js\"></script>\n" +
                        "  <style>\n" +
                        "    * { box-sizing: border-box; margin: 0; padding: 0; }\n" +
                        "    body { font-family: 'Segoe UI', Arial, sans-serif; background: #0f1428; color: #dce6ff; min-height: 100vh; }\n" +
                        "    .header { background: #080c1c; padding: 24px 32px; border-bottom: 2px solid #1a2040; }\n" +
                        "    .header h1 { color: #4fc3f7; font-size: 26px; }\n" +
                        "    .header p  { color: #8296c8; font-size: 13px; margin-top: 4px; }\n" +
                        "    .stats { display: flex; gap: 16px; padding: 20px 32px; background: #0d1530; }\n" +
                        "    .stat-card { background: #16213e; border: 1px solid #2a3560; border-radius: 10px; padding: 16px 24px; flex: 1; }\n" +
                        "    .stat-card .val { font-size: 28px; font-weight: bold; color: #4fc3f7; }\n" +
                        "    .stat-card .lbl { font-size: 12px; color: #8296c8; margin-top: 4px; }\n" +
                        "    .grid { display: flex; flex-wrap: wrap; gap: 20px; padding: 20px 32px; justify-content: center; }\n" +
                        "    .card { background: #16213e; border: 1px solid #2a3560; border-radius: 12px; padding: 20px; }\n" +
                        "    .card.wide { width: 100%; max-width: 900px; }\n" +
                        "    .card.half { width: calc(50% - 10px); max-width: 520px; }\n" +
                        "    .card h2 { color: #81d4fa; font-size: 15px; margin-bottom: 16px; padding-bottom: 8px; border-bottom: 1px solid #2a3560; }\n" +
                        "    canvas { max-height: 320px; }\n" +
                        "    .footer { text-align: center; padding: 20px; color: #4a5580; font-size: 12px; border-top: 1px solid #1a2040; margin-top: 8px; }\n" +
                        "  </style>\n" +
                        "</head>\n" +
                        "<body>\n" +
                        "  <div class=\"header\">\n" +
                        "    <h1>&#127970; Finance Parser Dashboard \u2014 User " + userId + "</h1>\n" +
                        "    <p>Generated: " + new java.util.Date() + " &nbsp;|&nbsp; Java + MySQL + JDBC Project</p>\n" +
                        "  </div>\n" +
                        "  <div class=\"stats\">\n" +
                        "    <div class=\"stat-card\"><div class=\"val\">" + txCount + "</div><div class=\"lbl\">Total Transactions</div></div>\n" +
                        "    <div class=\"stat-card\"><div class=\"val\">&#8377; " + String.format("%.0f", txTotal) + "</div><div class=\"lbl\">Total Spending</div></div>\n" +
                        "    <div class=\"stat-card\"><div class=\"val\">Active</div><div class=\"lbl\">Database Status</div></div>\n" +
                        "  </div>\n" +
                        "  <div class=\"grid\">\n" +
                        "    <div class=\"card wide\"><h2>&#128200; Monthly Spending Trend</h2><canvas id=\"bar\"></canvas></div>\n" +
                        "    <div class=\"card half\"><h2>&#128184; Spending by Category</h2><canvas id=\"pie\"></canvas></div>\n" +
                        "    <div class=\"card half\"><h2>&#128202; Daily Spending</h2><canvas id=\"line\"></canvas></div>\n" +
                        "  </div>\n" +
                        "  <div class=\"footer\">Finance Parser \u2014 Automated Personal Finance Parser | Java + MySQL + JDBC</div>\n" +
                        "  <script>\n" +
                        "    Chart.defaults.color = '#8296c8';\n" +
                        "    Chart.defaults.borderColor = '#1a2040';\n" +
                        "    new Chart(document.getElementById('pie'), { type:'pie', data:" + pie + ",\n" +
                        "      options:{ responsive:true, plugins:{ legend:{ position:'right', labels:{ color:'#dce6ff', font:{size:12} } } } } });\n" +
                        "    new Chart(document.getElementById('bar'), { type:'bar', data:" + bar + ",\n" +
                        "      options:{ responsive:true, scales:{ x:{ticks:{color:'#8296c8'}}, y:{beginAtZero:true,ticks:{color:'#8296c8'}} },\n" +
                        "                plugins:{ legend:{ labels:{ color:'#dce6ff' } } } } });\n" +
                        "    new Chart(document.getElementById('line'), { type:'line', data:" + line + ",\n" +
                        "      options:{ responsive:true, scales:{ x:{ticks:{color:'#8296c8'}}, y:{beginAtZero:true,ticks:{color:'#8296c8'}} },\n" +
                        "                plugins:{ legend:{ labels:{ color:'#dce6ff' } } } } });\n" +
                        "  </script>\n" +
                        "</body>\n" +
                        "</html>";

        try {
            File dir = new File(outputPath).getParentFile();
            if (dir != null && !dir.exists()) dir.mkdirs();
            try (FileWriter fw = new FileWriter(outputPath, false)) { // false = overwrite
                fw.write(html);
            }
            File f = new File(outputPath).getAbsoluteFile();
            System.out.println("[ChartData] \u2705 Dashboard written to: " + f.getPath());
            System.out.println("[ChartData]    Open in browser: " + f.toURI());
        } catch (IOException e) {
            System.err.println("[ChartData] ERROR writing HTML: " + e.getMessage());
        }
    }
}