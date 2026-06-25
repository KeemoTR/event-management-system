package eventsystem.service;

import eventsystem.dao.DepartmentDAO;
import eventsystem.dao.UserDAO;
import eventsystem.model.User;
import eventsystem.util.DBConnection;
import eventsystem.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Year;

public class AuthService {

    private static final String ROLE_STUDENT = "student";
    private static final String ROLE_ORGANIZER = "organizer";
    private static final String ROLE_ADMIN = "admin";

    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_BLOCKED = "blocked";

    private final UserDAO userDAO;
    private final DepartmentDAO departmentDAO;

    public AuthService() {
        this.userDAO = new UserDAO();
        this.departmentDAO = new DepartmentDAO();
    }

    public AuthService(UserDAO userDAO, DepartmentDAO departmentDAO) {
        this.userDAO = userDAO;
        this.departmentDAO = departmentDAO;
    }

    public User registerStudent(User user, String rawPassword) {
        if (user == null) {
            throw new ServiceException("User data is required.");
        }

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                trimUserFields(user);

                user.setEmail(normalizeEmail(user.getEmail()));
                user.setRole(ROLE_STUDENT);
                user.setStatus(STATUS_ACTIVE);

                validateCommonCreateInput(connection, user, rawPassword);
                validateStudentRegistrationInput(user);

                if (emailExists(connection, user.getEmail())) {
                    throw new ServiceException("Email already exists.");
                }

                user.setPasswordHash(PasswordUtil.hashPassword(rawPassword));

                boolean added = userDAO.addUser(user, connection);
                if (!added) {
                    throw new SQLException("Failed to create user.");
                }

                User createdUser = findUserByEmail(connection, user.getEmail());
                if (createdUser == null) {
                    throw new SQLException("User created but could not be loaded.");
                }

                createdUser.setPasswordHash(null);

                connection.commit();
                return createdUser;

            } catch (Exception e) {
                safeRollback(connection, e);
                throw wrapAsServiceException("Student registration failed.", e);
            }

        } catch (SQLException e) {
            throw new ServiceException("Database error while registering student.", e);
        }
    }

    public User createUserByAdmin(int adminId, User user, String rawPassword) {
        if (user == null) {
            throw new ServiceException("User data is required.");
        }

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                requireActiveAdmin(connection, adminId);

                trimUserFields(user);
                user.setEmail(normalizeEmail(user.getEmail()));

                validateCommonCreateInput(connection, user, rawPassword);
                validateRole(user.getRole());

                if (user.getStatus() == null || user.getStatus().trim().isEmpty()) {
                    user.setStatus(STATUS_ACTIVE);
                } else {
                    user.setStatus(user.getStatus().trim().toLowerCase());
                    validateStatus(user.getStatus());
                }

                if (emailExists(connection, user.getEmail())) {
                    throw new ServiceException("Email already exists.");
                }

                user.setPasswordHash(PasswordUtil.hashPassword(rawPassword));

                boolean added = userDAO.addUser(user, connection);
                if (!added) {
                    throw new SQLException("Failed to create user.");
                }

                User createdUser = findUserByEmail(connection, user.getEmail());
                if (createdUser == null) {
                    throw new SQLException("User created but could not be loaded.");
                }

                createdUser.setPasswordHash(null);

                connection.commit();
                return createdUser;

            } catch (Exception e) {
                safeRollback(connection, e);
                throw wrapAsServiceException("Admin user creation failed.", e);
            }

        } catch (SQLException e) {
            throw new ServiceException("Database error while creating user by admin.", e);
        }
    }

    public User register(User user, String rawPassword) {
        return registerStudent(user, rawPassword);
    }

    public User login(String email, String rawPassword) {
        String normalizedEmail = normalizeEmail(email);

        if (normalizedEmail == null || normalizedEmail.isEmpty()) {
            throw new ServiceException("Email is required.");
        }

        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new ServiceException("Password is required.");
        }

        User user = findUserByEmail(normalizedEmail);

        if (user == null) {
            throw new ServiceException("Invalid email or password.");
        }

        if (!matchesPassword(rawPassword, user.getPasswordHash())) {
            throw new ServiceException("Invalid email or password.");
        }

        if (STATUS_BLOCKED.equalsIgnoreCase(user.getStatus())) {
            throw new ServiceException("Blocked account cannot log in.");
        }

        user.setPasswordHash(null);
        return user;
    }

    public User findUserByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);

        if (normalizedEmail == null || normalizedEmail.isEmpty()) {
            throw new ServiceException("Email is required.");
        }

        try (Connection connection = DBConnection.getConnection()) {
            return findUserByEmail(connection, normalizedEmail);

        } catch (SQLException e) {
            throw new ServiceException("Database error while loading user by email.", e);
        }
    }

    public boolean emailExists(String email) {
        String normalizedEmail = normalizeEmail(email);

        if (normalizedEmail == null || normalizedEmail.isEmpty()) {
            throw new ServiceException("Email is required.");
        }

        try (Connection connection = DBConnection.getConnection()) {
            return emailExists(connection, normalizedEmail);

        } catch (SQLException e) {
            throw new ServiceException("Database error while checking email.", e);
        }
    }

    private void requireActiveAdmin(Connection connection, int adminId) {
        User admin = userDAO.getUserById(connection, adminId);

        if (admin == null) {
            throw new ServiceException("Admin user not found.");
        }

        if (!ROLE_ADMIN.equalsIgnoreCase(admin.getRole())) {
            throw new ServiceException("Only admin can create organizer or admin accounts.");
        }

        if (!STATUS_ACTIVE.equalsIgnoreCase(admin.getStatus())) {
            throw new ServiceException("Blocked admin cannot manage users.");
        }
    }

    private void validateCommonCreateInput(Connection connection, User user, String rawPassword) {
        if (user.getName() == null || user.getName().trim().isEmpty()) {
            throw new ServiceException("Name is required.");
        }

        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new ServiceException("Email is required.");
        }

        validateEmailFormat(user.getEmail());

        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new ServiceException("Password is required.");
        }

        if (rawPassword.trim().length() < 6) {
            throw new ServiceException("Password must be at least 6 characters.");
        }

        if (user.getFaculty() == null || user.getFaculty().trim().isEmpty()) {
            throw new ServiceException("Faculty is required.");
        }

        if (user.getDepartmentId() == null) {
            throw new ServiceException("Department is required.");
        }

        if (departmentDAO.getDepartmentById(connection, user.getDepartmentId()) == null) {
            throw new ServiceException("Department not found.");
        }

        validateAdmissionYear(user.getAdmissionYear());
    }

    private void validateStudentRegistrationInput(User user) {
        if (!ROLE_STUDENT.equalsIgnoreCase(user.getRole())) {
            throw new ServiceException("Self-registration is allowed for students only.");
        }
    }

    private void validateEmailFormat(String email) {
        String normalized = normalizeEmail(email);

        if (normalized == null
                || !normalized.contains("@")
                || normalized.startsWith("@")
                || normalized.endsWith("@")) {
            throw new ServiceException("Invalid email format.");
        }

        int atIndex = normalized.indexOf('@');
        String domainPart = normalized.substring(atIndex + 1);

        if (!domainPart.contains(".")) {
            throw new ServiceException("Invalid email format.");
        }
    }

    private void validateAdmissionYear(Integer admissionYear) {
        if (admissionYear == null) {
            throw new ServiceException("Admission year is required.");
        }

        int currentYear = Year.now().getValue();

        if (admissionYear < 1900 || admissionYear > currentYear + 1) {
            throw new ServiceException("Invalid admission year.");
        }
    }

    private void validateRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            throw new ServiceException("Role is required.");
        }

        String normalized = role.trim().toLowerCase();

        boolean valid = ROLE_STUDENT.equals(normalized)
                || ROLE_ORGANIZER.equals(normalized)
                || ROLE_ADMIN.equals(normalized);

        if (!valid) {
            throw new ServiceException("Invalid role.");
        }
    }

    private void validateStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            throw new ServiceException("Status is required.");
        }

        boolean valid = STATUS_ACTIVE.equalsIgnoreCase(status)
                || STATUS_BLOCKED.equalsIgnoreCase(status);

        if (!valid) {
            throw new ServiceException("Invalid status.");
        }
    }

    private User findUserByEmail(Connection connection, String email) {
        String sql = "SELECT * FROM users WHERE LOWER(email) = LOWER(?) LIMIT 1";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapUser(resultSet);
                }

                return null;
            }

        } catch (SQLException e) {
            throw new ServiceException("Database error while loading user by email.", e);
        }
    }

    private boolean emailExists(Connection connection, String email) {
        String sql = "SELECT 1 FROM users WHERE LOWER(email) = LOWER(?) LIMIT 1";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }

        } catch (SQLException e) {
            throw new ServiceException("Database error while checking email.", e);
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

    private void trimUserFields(User user) {
        if (user.getName() != null) {
            user.setName(user.getName().trim());
        }

        if (user.getEmail() != null) {
            user.setEmail(user.getEmail().trim());
        }

        if (user.getFaculty() != null) {
            user.setFaculty(user.getFaculty().trim());
        }

        if (user.getRole() != null) {
            user.setRole(user.getRole().trim().toLowerCase());
        }

        if (user.getStatus() != null) {
            user.setStatus(user.getStatus().trim().toLowerCase());
        }
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }

        return email.trim().toLowerCase();
    }

    private boolean matchesPassword(String rawPassword, String storedPasswordHash) {
        return PasswordUtil.verifyPassword(rawPassword, storedPasswordHash);
    }

    private void safeRollback(Connection connection, Exception originalException) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            originalException.addSuppressed(rollbackException);
        }
    }

    private ServiceException wrapAsServiceException(String defaultMessage, Exception exception) {
        if (exception instanceof ServiceException) {
            return (ServiceException) exception;
        }

        return new ServiceException(defaultMessage, exception);
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