package db;

import java.sql.*;

// DBConnection — Singleton JDBC connection to MySQL
// One shared connection object reused across the entire application

public class DBConnection {

    // ── CHANGE THESE TO MATCH YOUR MYSQL SETUP ───────────────────
    private static final String DB_URL =
            "jdbc:mysql://localhost:3306/finance_db?useSSL=false&allowPublicKeyRetrieval=true";
    // jdbc:mysql → MySQL JDBC driver
    // localhost   → MySQL is running on same machine
    // 3306        → MySQL default port
    // finance_db  → the database created in 01_create_database.sql
    private static final String DB_USER     = "root";
    private static final String DB_PASSWORD = "root";   // ← CHANGE THIS

    private static Connection connection = null;

    // Returns existing connection or creates a new one
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                // Load MySQL JDBC driver class into JVM memory
                Class.forName("com.mysql.cj.jdbc.Driver");
                // Create the actual connection to MySQL
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                System.out.println("[DBConnection] Connected to MySQL successfully!");
            } catch (ClassNotFoundException e) {
                System.err.println("ERROR: MySQL JDBC driver not found!");
                System.err.println("Fix: Add mysql-connector-java.jar to lib/ folder and classpath.");
                throw new SQLException(e);
            }
        }
        return connection;
    }

    // Call when application finishes — closes the DB connection cleanly
    public static void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DBConnection] Connection closed.");
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }
}
