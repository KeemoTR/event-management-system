package eventsystem.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String URL = "jdbc:mysql://localhost:3306/event_system";
    private static final String USER = "root";
    private static final String PASSWORD = "Kamel_2004";

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found inside Tomcat runtime.", e);
        }

        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}