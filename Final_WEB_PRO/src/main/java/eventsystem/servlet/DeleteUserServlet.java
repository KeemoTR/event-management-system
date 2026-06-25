package eventsystem.servlet;

import eventsystem.model.User;
import eventsystem.util.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet("/delete-user")
public class DeleteUserServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);

        try {
            if (session == null || !(session.getAttribute("user") instanceof User)) {
                response.sendRedirect(request.getContextPath() + "/login?next=/admin-users");
                return;
            }

            User loggedInUser = (User) session.getAttribute("user");

            if (!"admin".equalsIgnoreCase(loggedInUser.getRole())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Only admins can delete users.");
                return;
            }

            String userIdText = request.getParameter("userId");

            if (userIdText == null || userIdText.trim().isEmpty()) {
                session.setAttribute("error", "User ID is required.");
                response.sendRedirect(request.getContextPath() + "/admin-users");
                return;
            }

            int targetUserId = Integer.parseInt(userIdText.trim());

            deleteUserSafely(loggedInUser.getId(), targetUserId);

            response.sendRedirect(request.getContextPath() + "/admin-users?deleted=success");

        } catch (Exception e) {
            if (session != null) {
                session.setAttribute("error", e.getMessage());
            }

            response.sendRedirect(request.getContextPath() + "/admin-users");
        }
    }

    private void deleteUserSafely(int adminId, int targetUserId) {
        Connection connection = null;

        try {
            connection = DBConnection.getConnection();
            connection.setAutoCommit(false);

            UserSnapshot targetUser = findUserById(connection, targetUserId);

            if (targetUser == null) {
                throw new IllegalArgumentException("Target user not found.");
            }

            if (adminId == targetUserId) {
                throw new IllegalArgumentException("You cannot delete your own admin account.");
            }

            if ("admin".equalsIgnoreCase(targetUser.role)) {
                throw new IllegalArgumentException("Admin accounts are protected and cannot be deleted.");
            }

            if ("organizer".equalsIgnoreCase(targetUser.role)) {
                int ownedEvents = countEventsOwnedByOrganizer(connection, targetUserId);

                if (ownedEvents > 0) {
                    throw new IllegalArgumentException(
                            "Cannot delete this organizer because they own events. Delete or reassign their events first."
                    );
                }
            }

            restoreSeatsForActiveReservations(connection, targetUserId);

            if (tableExists(connection, "ratings")) {
                deleteRatingsForUser(connection, targetUserId);
            }

            deleteReservationsForUser(connection, targetUserId);

            int deletedRows = deleteUser(connection, targetUserId);

            if (deletedRows == 0) {
                throw new IllegalArgumentException("User could not be deleted.");
            }

            connection.commit();

        } catch (Exception e) {
            rollbackQuietly(connection);

            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }

            throw new RuntimeException("Failed to delete user.", e);

        } finally {
            resetAutoCommitAndClose(connection);
        }
    }

    private UserSnapshot findUserById(Connection connection, int userId) throws Exception {
        String sql =
                "SELECT id, name, email, role, status " +
                "FROM users " +
                "WHERE id = ? " +
                "LIMIT 1";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    UserSnapshot user = new UserSnapshot();
                    user.id = resultSet.getInt("id");
                    user.name = resultSet.getString("name");
                    user.email = resultSet.getString("email");
                    user.role = resultSet.getString("role");
                    user.status = resultSet.getString("status");
                    return user;
                }
            }
        }

        return null;
    }

    private int countEventsOwnedByOrganizer(Connection connection, int organizerId) throws Exception {
        String sql =
                "SELECT COUNT(*) AS event_count " +
                "FROM events " +
                "WHERE organizer_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, organizerId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("event_count");
                }
            }
        }

        return 0;
    }

    private void restoreSeatsForActiveReservations(Connection connection, int studentId) throws Exception {
        String sql =
                "UPDATE events e " +
                "JOIN reservations r ON r.event_id = e.id " +
                "SET e.remaining_seats = CASE " +
                "    WHEN e.remaining_seats < e.capacity THEN e.remaining_seats + 1 " +
                "    ELSE e.remaining_seats " +
                "END " +
                "WHERE r.student_id = ? " +
                "AND r.reservation_status = 'reserved'";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, studentId);
            statement.executeUpdate();
        }
    }

    private void deleteRatingsForUser(Connection connection, int studentId) throws Exception {
        String sql =
                "DELETE FROM ratings " +
                "WHERE student_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, studentId);
            statement.executeUpdate();
        }
    }

    private void deleteReservationsForUser(Connection connection, int studentId) throws Exception {
        String sql =
                "DELETE FROM reservations " +
                "WHERE student_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, studentId);
            statement.executeUpdate();
        }
    }

    private int deleteUser(Connection connection, int userId) throws Exception {
        String sql =
                "DELETE FROM users " +
                "WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            return statement.executeUpdate();
        }
    }

    private boolean tableExists(Connection connection, String tableName) {
        try {
            DatabaseMetaData metaData = connection.getMetaData();

            try (ResultSet resultSet = metaData.getTables(
                    connection.getCatalog(),
                    null,
                    tableName,
                    new String[]{"TABLE"}
            )) {
                return resultSet.next();
            }

        } catch (Exception e) {
            return false;
        }
    }

    private void rollbackQuietly(Connection connection) {
        if (connection != null) {
            try {
                connection.rollback();
            } catch (Exception ignored) {
            }
        }
    }

    private void resetAutoCommitAndClose(Connection connection) {
        if (connection != null) {
            try {
                connection.setAutoCommit(true);
            } catch (Exception ignored) {
            }

            try {
                connection.close();
            } catch (Exception ignored) {
            }
        }
    }

    private static class UserSnapshot {
        private int id;
        private String name;
        private String email;
        private String role;
        private String status;
    }
}