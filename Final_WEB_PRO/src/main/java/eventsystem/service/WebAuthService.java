package eventsystem.service;

import eventsystem.model.User;
import eventsystem.util.DBConnection;
import eventsystem.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class WebAuthService {

    private static final String ROLE_STUDENT = "student";
    private static final String STATUS_ACTIVE = "active";

    public User login(String email, String password) {
        String cleanEmail = cleanEmail(email);

        if (cleanEmail.isEmpty() || isBlank(password)) {
            throw new ServiceException("Email and password are required.");
        }

        try (Connection connection = DBConnection.getConnection()) {
            User user = findUserByEmail(connection, cleanEmail);

            if (user == null || !PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
                throw new ServiceException("Invalid email or password.");
            }

            if (!STATUS_ACTIVE.equalsIgnoreCase(user.getStatus())) {
                throw new ServiceException("Your account is blocked. Please contact the admin.");
            }

            user.setPasswordHash(null);
            return user;

        } catch (SQLException e) {
            throw new ServiceException("Database error while logging in.", e);
        }
    }

    public User registerStudent(
            String name,
            String email,
            String password,
            String faculty,
            Integer departmentId,
            Integer admissionYear
    ) {
        String cleanName = cleanText(name);
        String cleanEmail = cleanEmail(email);
        String cleanFaculty = cleanText(faculty);

        validateRegistration(
                cleanName,
                cleanEmail,
                password,
                cleanFaculty,
                departmentId,
                admissionYear
        );

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                if (emailExists(connection, cleanEmail)) {
                    throw new ServiceException("Email is already registered.");
                }

                String hashedPassword = PasswordUtil.hashPassword(password);

                insertStudent(
                        connection,
                        cleanName,
                        cleanEmail,
                        hashedPassword,
                        cleanFaculty,
                        departmentId,
                        admissionYear
                );

                User createdUser = findUserByEmail(connection, cleanEmail);

                if (createdUser == null) {
                    throw new SQLException("User was inserted but could not be loaded.");
                }

                connection.commit();

                createdUser.setPasswordHash(null);
                return createdUser;

            } catch (Exception e) {
                rollback(connection, e);
                throw wrap(e);
            }

        } catch (SQLException e) {
            throw new ServiceException("Database error while registering.", e);
        }
    }

    private void validateRegistration(
            String name,
            String email,
            String password,
            String faculty,
            Integer departmentId,
            Integer admissionYear
    ) {
        if (isBlank(name)) {
            throw new ServiceException("Name is required.");
        }

        if (!isValidEmail(email)) {
            throw new ServiceException("Please enter a valid email address.");
        }

        if (isBlank(password) || password.length() < 6) {
            throw new ServiceException("Password must be at least 6 characters.");
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

    private User findUserByEmail(Connection connection, String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE LOWER(email) = LOWER(?) LIMIT 1";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapUser(resultSet);
                }
            }
        }

        return null;
    }

    private boolean emailExists(Connection connection, String email) throws SQLException {
        String sql = "SELECT id FROM users WHERE LOWER(email) = LOWER(?) LIMIT 1";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void insertStudent(
            Connection connection,
            String name,
            String email,
            String passwordHash,
            String faculty,
            Integer departmentId,
            Integer admissionYear
    ) throws SQLException {

        String sql =
                "INSERT INTO users " +
                "(name, email, password_hash, role, status, faculty, department_id, admission_year) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setString(2, email);
            statement.setString(3, passwordHash);
            statement.setString(4, ROLE_STUDENT);
            statement.setString(5, STATUS_ACTIVE);
            statement.setString(6, faculty);
            statement.setInt(7, departmentId);
            statement.setInt(8, admissionYear);

            statement.executeUpdate();
        }
    }

    private User mapUser(ResultSet resultSet) throws SQLException {
        User user = new User();

        user.setId(resultSet.getInt("id"));
        user.setName(resultSet.getString("name"));
        user.setEmail(resultSet.getString("email"));
        user.setPasswordHash(resultSet.getString("password_hash"));
        user.setRole(resultSet.getString("role"));
        user.setStatus(resultSet.getString("status"));
        user.setFaculty(resultSet.getString("faculty"));

        int departmentId = resultSet.getInt("department_id");
        if (!resultSet.wasNull()) {
            user.setDepartmentId(departmentId);
        }

        int admissionYear = resultSet.getInt("admission_year");
        if (!resultSet.wasNull()) {
            user.setAdmissionYear(admissionYear);
        }

        return user;
    }

    private String cleanText(String value) {
        return value == null ? "" : value.trim();
    }

    private String cleanEmail(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    private void rollback(Connection connection, Exception originalException) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            originalException.addSuppressed(rollbackException);
        }
    }

    private ServiceException wrap(Exception e) {
        if (e instanceof ServiceException) {
            return (ServiceException) e;
        }

        return new ServiceException("Authentication operation failed.", e);
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