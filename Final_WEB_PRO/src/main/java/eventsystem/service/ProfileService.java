package eventsystem.service;

import eventsystem.model.User;
import eventsystem.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ProfileService {

    public User getProfile(int userId) {
        try (Connection connection = DBConnection.getConnection()) {
            User user = findUserById(connection, userId);

            if (user == null) {
                throw new ServiceException("User profile not found.");
            }

            user.setPasswordHash(null);
            return user;

        } catch (SQLException e) {
            throw new ServiceException("Database error while loading profile.", e);
        }
    }

    public User updateProfile(
            int userId,
            String name,
            String faculty,
            Integer departmentId,
            Integer admissionYear
    ) {
        String cleanName = cleanText(name);
        String cleanFaculty = cleanText(faculty);

        validateProfile(cleanName, cleanFaculty, departmentId, admissionYear);

        try (Connection connection = DBConnection.getConnection()) {
            String sql =
                    "UPDATE users " +
                    "SET name = ?, faculty = ?, department_id = ?, admission_year = ? " +
                    "WHERE id = ?";

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, cleanName);
                statement.setString(2, cleanFaculty);
                statement.setInt(3, departmentId);
                statement.setInt(4, admissionYear);
                statement.setInt(5, userId);

                int rows = statement.executeUpdate();

                if (rows == 0) {
                    throw new ServiceException("Profile could not be updated.");
                }
            }

            User updatedUser = findUserById(connection, userId);

            if (updatedUser == null) {
                throw new ServiceException("Updated profile could not be loaded.");
            }

            updatedUser.setPasswordHash(null);
            return updatedUser;

        } catch (SQLException e) {
            throw new ServiceException("Database error while updating profile.", e);
        }
    }

    private User findUserById(Connection connection, int userId) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ? LIMIT 1";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        }

        return null;
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();

        user.setId(rs.getInt("id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(rs.getString("role"));
        user.setStatus(rs.getString("status"));
        user.setFaculty(rs.getString("faculty"));

        int departmentId = rs.getInt("department_id");
        if (!rs.wasNull()) {
            user.setDepartmentId(departmentId);
        }

        int admissionYear = rs.getInt("admission_year");
        if (!rs.wasNull()) {
            user.setAdmissionYear(admissionYear);
        }

        return user;
    }

    private void validateProfile(
            String name,
            String faculty,
            Integer departmentId,
            Integer admissionYear
    ) {
        if (isBlank(name)) {
            throw new ServiceException("Name is required.");
        }

        if (isBlank(faculty)) {
            throw new ServiceException("Faculty is required.");
        }

        if (departmentId == null || departmentId <= 0) {
            throw new ServiceException("Department is required.");
        }

        if (admissionYear == null || admissionYear < 2000 || admissionYear > 2100) {
            throw new ServiceException("Admission year is invalid.");
        }
    }

    private String cleanText(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static class ServiceException extends RuntimeException {

        public ServiceException(String message) {
            super(message);
        }

        public ServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}