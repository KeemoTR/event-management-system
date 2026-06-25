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
import java.sql.*;
import java.util.*;

@WebServlet("/organizer/event-ratings")
public class OrganizerEventRatingsServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = getLoggedInUser(request);

        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        if (!"organizer".equalsIgnoreCase(user.getRole())
                && !"admin".equalsIgnoreCase(user.getRole())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "You are not allowed to view event ratings.");
            return;
        }

        try {
            int eventId = parseInteger(request.getParameter("eventId"), "Event id is required.");

            try (Connection connection = DBConnection.getConnection()) {

                Map<String, Object> event = getEventForRatings(
                        connection,
                        eventId,
                        user.getId(),
                        user.getRole()
                );

                if (event == null) {
                    throw new RuntimeException("Event not found or you are not allowed to view its ratings.");
                }

                List<Map<String, Object>> ratings = getRatingsForEvent(connection, eventId);

                double averageRating = calculateAverage(ratings);
                int totalRatings = ratings.size();

                request.setAttribute("event", event);
                request.setAttribute("ratings", ratings);
                request.setAttribute("averageRating", averageRating);
                request.setAttribute("totalRatings", totalRatings);

                request.getRequestDispatcher("/organizer-event-ratings.jsp")
                        .forward(request, response);
            }

        } catch (Exception e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/organizer-event-ratings.jsp")
                    .forward(request, response);
        }
    }

    private Map<String, Object> getEventForRatings(
            Connection connection,
            int eventId,
            int actorId,
            String actorRole
    ) throws SQLException {

        String sql =
                "SELECT id, title, event_type, status, event_date_time, location, organizer_id " +
                "FROM events " +
                "WHERE id = ? " +
                "AND (? = 'admin' OR organizer_id = ?) " +
                "LIMIT 1";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, eventId);
            statement.setString(2, actorRole.toLowerCase());
            statement.setInt(3, actorId);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> event = new HashMap<>();

                    event.put("id", rs.getInt("id"));
                    event.put("title", rs.getString("title"));
                    event.put("eventType", rs.getString("event_type"));
                    event.put("status", rs.getString("status"));
                    event.put("eventDateTime", rs.getTimestamp("event_date_time"));
                    event.put("location", rs.getString("location"));
                    event.put("organizerId", rs.getInt("organizer_id"));

                    return event;
                }
            }
        }

        return null;
    }

    private List<Map<String, Object>> getRatingsForEvent(Connection connection, int eventId)
            throws SQLException {

        String sql =
                "SELECT " +
                "r.id AS rating_id, " +
                "r.rating, " +
                "r.comment, " +
                "r.created_at, " +
                "u.name AS student_name, " +
                "u.email AS student_email " +
                "FROM ratings r " +
                "JOIN users u ON r.student_id = u.id " +
                "WHERE r.event_id = ? " +
                "ORDER BY r.created_at DESC";

        List<Map<String, Object>> ratings = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, eventId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();

                    row.put("ratingId", rs.getInt("rating_id"));
                    row.put("rating", rs.getInt("rating"));
                    row.put("comment", rs.getString("comment"));
                    row.put("createdAt", rs.getTimestamp("created_at"));
                    row.put("studentName", rs.getString("student_name"));
                    row.put("studentEmail", rs.getString("student_email"));

                    ratings.add(row);
                }
            }
        }

        return ratings;
    }

    private double calculateAverage(List<Map<String, Object>> ratings) {
        if (ratings == null || ratings.isEmpty()) {
            return 0.0;
        }

        int total = 0;

        for (Map<String, Object> rating : ratings) {
            total += (Integer) rating.get("rating");
        }

        return (double) total / ratings.size();
    }

    private User getLoggedInUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session != null && session.getAttribute("user") instanceof User) {
            return (User) session.getAttribute("user");
        }

        return null;
    }

    private int parseInteger(String value, String errorMessage) {
        try {
            if (value == null || value.trim().isEmpty()) {
                throw new NumberFormatException();
            }

            return Integer.parseInt(value.trim());

        } catch (NumberFormatException e) {
            throw new RuntimeException(errorMessage);
        }
    }
}