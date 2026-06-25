package eventsystem.util;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class CreatePbkdf2UserTest {

    public static void main(String[] args) {

        String hashedPassword = PasswordUtil.hashPassword("123456");

        String sql = """
                INSERT INTO users
                (name, email, password_hash, role, status, faculty, department_id, admission_year)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "PBKDF2 Test User");
            ps.setString(2, "pbkdf2_test_user@test.com");
            ps.setString(3, hashedPassword);
            ps.setString(4, "student");
            ps.setString(5, "active");
            ps.setString(6, "IT");
            ps.setInt(7, 1);
            ps.setInt(8, 2022);

            int rows = ps.executeUpdate();

            System.out.println("Rows inserted: " + rows);
            System.out.println("Stored hash:");
            System.out.println(hashedPassword);

            if (hashedPassword.startsWith("pbkdf2_sha256$")) {
                System.out.println("PBKDF2 storage test PASSED.");
            } else {
                System.out.println("PBKDF2 storage test FAILED.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}