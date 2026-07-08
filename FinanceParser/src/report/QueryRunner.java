package report;
import db.DBConnection;
import java.sql.*;
// QueryRunner — runs SQL reports and prints formatted output to console
public class QueryRunner {
    public static void runAllReports(int userId) {
        System.out.println(" "+"=".repeat(55)+"FINANCE REPORT — User ID: "+userId+" "+"=".repeat(55));
        reportByCategory(userId); reportMonthly(userId); reportBudget(userId,"2024-10"); reportTopMerchants(userId); reportDebitCredit();
    }

    private static void reportByCategory(int userId) {
        System.out.println(" --- Spending by Category ---");
        try {
            PreparedStatement ps=DBConnection.getConnection().prepareStatement(
                    "SELECT c.category_name,SUM(t.amount) AS total FROM TRANSACTION t "+
                            "JOIN CATEGORY c ON t.category_id=c.category_id WHERE t.user_id=? "+
                            "GROUP BY c.category_name ORDER BY total DESC");
            ps.setInt(1,userId); ResultSet rs=ps.executeQuery();
            System.out.printf("%-20s %10s%n","Category","Total (INR)"); System.out.println("-".repeat(32));
            while(rs.next()) System.out.printf("%-20s %10.2f%n",rs.getString("category_name"),rs.getDouble("total"));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private static void reportMonthly(int userId) {
        System.out.println(" --- Monthly Trend ---");
        try {
            PreparedStatement ps=DBConnection.getConnection().prepareStatement(
                    "SELECT DATE_FORMAT(transaction_date,'%Y-%m') AS m,COUNT(*) AS n,SUM(amount) AS t "+
                            "FROM TRANSACTION WHERE user_id=? GROUP BY m ORDER BY m");
            ps.setInt(1,userId); ResultSet rs=ps.executeQuery();
            System.out.printf("%-12s %6s %12s%n","Month","Count","Total"); System.out.println("-".repeat(32));
            while(rs.next()) System.out.printf("%-12s %6d %12.2f%n",rs.getString("m"),rs.getInt("n"),rs.getDouble("t"));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private static void reportBudget(int userId, String period) {
        System.out.println(" --- Budget vs Actual ("+period+") ---");
        try {
            PreparedStatement ps=DBConnection.getConnection().prepareStatement(
                    "SELECT c.category_name,b.limit_amount AS budget,COALESCE(SUM(t.amount),0) AS spent,"+
                            "(b.limit_amount-COALESCE(SUM(t.amount),0)) AS rem FROM BUDGET b "+
                            "JOIN CATEGORY c ON b.category_id=c.category_id "+
                            "LEFT JOIN TRANSACTION t ON t.category_id=c.category_id AND t.user_id=b.user_id "+
                            "AND DATE_FORMAT(t.transaction_date,'%Y-%m')=? WHERE b.user_id=? AND b.period=? "+
                            "GROUP BY c.category_name,b.limit_amount");
            ps.setString(1,period); ps.setInt(2,userId); ps.setString(3,period);
            ResultSet rs=ps.executeQuery();
            System.out.printf("%-18s %8s %8s %10s%n","Category","Budget","Spent","Remaining"); System.out.println("-".repeat(48));
            while(rs.next()) { double rem=rs.getDouble("rem");
                System.out.printf("%-18s %8.0f %8.0f %10.0f %s%n",rs.getString("category_name"),
                        rs.getDouble("budget"),rs.getDouble("spent"),rem,rem<0?"⚠ OVER BUDGET":""); }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private static void reportTopMerchants(int userId) {
        System.out.println(" --- Top 5 Merchants ---");
        try {
            PreparedStatement ps=DBConnection.getConnection().prepareStatement(
                    "SELECT m.merchant_name,COUNT(*) AS v,SUM(t.amount) AS total FROM TRANSACTION t "+
                            "JOIN MERCHANT m ON t.merchant_id=m.merchant_id WHERE t.user_id=? "+
                            "GROUP BY m.merchant_name ORDER BY total DESC LIMIT 5");
            ps.setInt(1,userId); ResultSet rs=ps.executeQuery();
            System.out.printf("%-22s %6s %12s%n","Merchant","Visits","Total"); System.out.println("-".repeat(42));
            while(rs.next()) System.out.printf("%-22s %6d %12.2f%n",rs.getString("merchant_name"),rs.getInt("v"),rs.getDouble("total"));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private static void reportDebitCredit() {
        System.out.println(" --- DEBIT vs CREDIT ---");
        try {
            ResultSet rs=DBConnection.getConnection().createStatement().executeQuery(
                    "SELECT tx_type,COUNT(*) AS c,SUM(amount) AS t FROM BANK_SMS WHERE parse_status='parsed' GROUP BY tx_type");
            while(rs.next()) System.out.printf("%-8s count=%-4d total=%.2f%n",rs.getString("tx_type"),rs.getInt("c"),rs.getDouble("t"));
        } catch (SQLException e) { e.printStackTrace(); }
    }
}
