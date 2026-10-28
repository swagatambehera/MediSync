package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Centralized JDBC configuration. Environment variables override local defaults. */
public final class DBConnection {
    private static final String URL = setting("MEDICINE_DB_URL", "jdbc:mysql://localhost:3306/medicine_system?serverTimezone=UTC");
    private static final String USER = setting("MEDICINE_DB_USER", "root");
    private static final String PASSWORD = setting("MEDICINE_DB_PASSWORD", "");

    private DBConnection() { }

    private static String setting(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static String getConfiguredUrl() { return URL; }
}
