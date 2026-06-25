package eventsystem.util;

import java.sql.Connection;

public class TestConnection {

    public static void main(String[] args) {

        try {
            Connection conn = DBConnection.getConnection();

            if (conn != null) {
                System.out.println("Connected successfully ✅");
            }

        } catch (Exception e) {
            System.out.println("Connection failed ❌");
            e.printStackTrace();
        }
    }
}