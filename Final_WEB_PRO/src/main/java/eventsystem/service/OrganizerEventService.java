package eventsystem.service;

import eventsystem.model.Event;
import eventsystem.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class OrganizerEventService {

    private static final String ROLE_ADMIN = "admin";
    private static final String ROLE_ORGANIZER = "organizer";

    private static final String STATUS_OPEN = "open";
    private static final String STATUS_CLOSED = "closed";
    private static final String STATUS_COMPLETED = "completed";
    private static final String STATUS_EXPIRED = "expired";

    private static final String ATTENDANCE_PRESENT = "present";
    private static final String ATTENDANCE_ABSENT = "absent";

    private static final String RESERVATION_RESERVED = "reserved";

    public List<Map<String, Object>> getEventAttendees(int actorId, String actorRole, int eventId) {
        try (Connection connection = DBConnection.getConnection()) {

            ensureCanManageEvent(connection, actorId, actorRole, eventId);

            String sql =
                    "SELECT " +
                    "r.id AS reservation_id, " +
                    "r.student_id, " +
                    "u.name AS student_name, " +
                    "u.email AS student_email, " +
                    "r.reservation_status, " +
                    "r.attendance_status, " +
                    "r.reserved_at " +
                    "FROM reservations r " +
                    "JOIN users u ON r.student_id = u.id " +
                    "WHERE r.event_id = ? " +
                    "ORDER BY r.reserved_at DESC";

            List<Map<String, Object>> attendees = new ArrayList<>();

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, eventId);

                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> row = new HashMap<>();

                        row.put("reservationId", rs.getInt("reservation_id"));
                        row.put("studentId", rs.getInt("student_id"));
                        row.put("studentName", rs.getString("student_name"));
                        row.put("studentEmail", rs.getString("student_email"));
                        row.put("reservationStatus", rs.getString("reservation_status"));
                        row.put("attendanceStatus", rs.getString("attendance_status"));
                        row.put("reservedAt", rs.getTimestamp("reserved_at"));

                        attendees.add(row);
                    }
                }
            }

            return attendees;

        } catch (SQLException e) {
            throw new ServiceException("Database error while loading attendees.", e);
        }
    }

    public Event getEventForManagement(int actorId, String actorRole, int eventId) {
        try (Connection connection = DBConnection.getConnection()) {

            ensureCanManageEvent(connection, actorId, actorRole, eventId);

            Event event = findEventById(connection, eventId);

            if (event == null) {
                throw new ServiceException("Event not found.");
            }

            return event;

        } catch (SQLException e) {
            throw new ServiceException("Database error while loading event.", e);
        }
    }

    public void markAttendance(
            int actorId,
            String actorRole,
            int eventId,
            int reservationId,
            String attendanceStatus
    ) {
        String cleanStatus = cleanText(attendanceStatus).toLowerCase();

        if (!ATTENDANCE_PRESENT.equals(cleanStatus) && !ATTENDANCE_ABSENT.equals(cleanStatus)) {
            throw new ServiceException("Attendance status must be present or absent.");
        }

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                ensureCanManageEvent(connection, actorId, actorRole, eventId);

                Event event = findEventById(connection, eventId);
                if (event == null) {
                    throw new ServiceException("Event not found.");
                }

                if (event.getEventDateTime() == null) {
                    throw new ServiceException("Event date and time is missing.");
                }

                boolean eventStarted =
                        !event.getEventDateTime().isAfter(LocalDateTime.now());

                boolean completed =
                        STATUS_COMPLETED.equalsIgnoreCase(event.getStatus());

                if (!eventStarted && !completed) {
                    throw new ServiceException("Attendance cannot be marked before the event starts.");
                }

                String sql =
                        "UPDATE reservations " +
                        "SET attendance_status = ? " +
                        "WHERE id = ? " +
                        "AND event_id = ? " +
                        "AND reservation_status = ?";

                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, cleanStatus);
                    statement.setInt(2, reservationId);
                    statement.setInt(3, eventId);
                    statement.setString(4, RESERVATION_RESERVED);

                    int rows = statement.executeUpdate();

                    if (rows == 0) {
                        throw new ServiceException(
                                "Attendance could not be updated. Reservation may be cancelled or not found."
                        );
                    }
                }

                connection.commit();

            } catch (Exception e) {
                rollback(connection, e);
                throw wrap(e);
            }

        } catch (SQLException e) {
            throw new ServiceException("Database error while updating attendance.", e);
        }
    }

    public void updateEvent(
            int actorId,
            String actorRole,
            int eventId,
            String title,
            String description,
            Integer departmentId,
            LocalDateTime eventDateTime,
            String location,
            Integer capacity,
            Integer categoryId,
            String eventType,
            String imagePath
    ) {
        String cleanTitle = cleanText(title);
        String cleanDescription = cleanText(description);
        String cleanLocation = cleanText(location);
        String cleanEventType = cleanText(eventType).toLowerCase();
        String cleanImagePath = cleanText(imagePath);

        validateEventInputs(
                cleanTitle,
                cleanDescription,
                departmentId,
                eventDateTime,
                cleanLocation,
                capacity,
                categoryId,
                cleanEventType
        );

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                ensureCanManageEvent(connection, actorId, actorRole, eventId);

                Event existingEvent = findEventById(connection, eventId);
                if (existingEvent == null) {
                    throw new ServiceException("Event not found.");
                }

                if (STATUS_COMPLETED.equalsIgnoreCase(existingEvent.getStatus())) {
                    throw new ServiceException("Completed events cannot be edited.");
                }

                int activeReservations = countActiveReservations(connection, eventId);

                if (capacity < activeReservations) {
                    throw new ServiceException("Capacity cannot be less than active reservations.");
                }

                int newRemainingSeats = capacity - activeReservations;

                String newStatus = determineStatusAfterEdit(existingEvent, eventDateTime);

                String sql =
                        "UPDATE events " +
                        "SET title = ?, " +
                        "description = ?, " +
                        "department_id = ?, " +
                        "event_date_time = ?, " +
                        "location = ?, " +
                        "capacity = ?, " +
                        "remaining_seats = ?, " +
                        "category_id = ?, " +
                        "event_type = ?, " +
                        "image_path = ?, " +
                        "status = ? " +
                        "WHERE id = ?";

                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, cleanTitle);
                    statement.setString(2, cleanDescription);
                    statement.setInt(3, departmentId);
                    statement.setTimestamp(4, Timestamp.valueOf(eventDateTime));
                    statement.setString(5, cleanLocation);
                    statement.setInt(6, capacity);
                    statement.setInt(7, newRemainingSeats);
                    statement.setInt(8, categoryId);
                    statement.setString(9, cleanEventType);

                    if (cleanImagePath.isEmpty()) {
                        statement.setNull(10, Types.VARCHAR);
                    } else {
                        statement.setString(10, cleanImagePath);
                    }

                    statement.setString(11, newStatus);
                    statement.setInt(12, eventId);

                    int rows = statement.executeUpdate();

                    if (rows == 0) {
                        throw new ServiceException("Event could not be updated.");
                    }
                }

                connection.commit();

            } catch (Exception e) {
                rollback(connection, e);
                throw wrap(e);
            }

        } catch (SQLException e) {
            throw new ServiceException("Database error while updating event.", e);
        }
    }

    private String determineStatusAfterEdit(Event existingEvent, LocalDateTime newEventDateTime) {
        String currentStatus = cleanText(existingEvent.getStatus()).toLowerCase();

        if (STATUS_COMPLETED.equals(currentStatus)) {
            throw new ServiceException("Completed events cannot be edited.");
        }

        if (STATUS_EXPIRED.equals(currentStatus)) {
            if (newEventDateTime.isAfter(LocalDateTime.now())) {
                return STATUS_OPEN;
            }

            return STATUS_EXPIRED;
        }

        if (STATUS_OPEN.equals(currentStatus)) {
            if (!newEventDateTime.isAfter(LocalDateTime.now())) {
                return STATUS_EXPIRED;
            }

            return STATUS_OPEN;
        }

        if (STATUS_CLOSED.equals(currentStatus)) {
            if (!newEventDateTime.isAfter(LocalDateTime.now())) {
                return STATUS_EXPIRED;
            }

            return STATUS_CLOSED;
        }

        throw new ServiceException("Invalid event status.");
    }

    private void ensureCanManageEvent(
            Connection connection,
            int actorId,
            String actorRole,
            int eventId
    ) throws SQLException {
        String cleanRole = cleanText(actorRole).toLowerCase();

        if (ROLE_ADMIN.equals(cleanRole)) {
            if (findEventById(connection, eventId) == null) {
                throw new ServiceException("Event not found.");
            }

            return;
        }

        if (!ROLE_ORGANIZER.equals(cleanRole)) {
            throw new ServiceException("You are not allowed to manage events.");
        }

        String sql =
                "SELECT id " +
                "FROM events " +
                "WHERE id = ? AND organizer_id = ? " +
                "LIMIT 1";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, eventId);
            statement.setInt(2, actorId);

            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new ServiceException("You are not allowed to manage this event.");
                }
            }
        }
    }

    private Event findEventById(Connection connection, int eventId) throws SQLException {
        String sql = "SELECT * FROM events WHERE id = ? LIMIT 1";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, eventId);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapEvent(rs);
                }
            }
        }

        return null;
    }

    private int countActiveReservations(Connection connection, int eventId) throws SQLException {
        String sql =
                "SELECT COUNT(*) " +
                "FROM reservations " +
                "WHERE event_id = ? " +
                "AND reservation_status = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, eventId);
            statement.setString(2, RESERVATION_RESERVED);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        return 0;
    }

    private Event mapEvent(ResultSet rs) throws SQLException {
        Event event = new Event();

        event.setId(rs.getInt("id"));
        event.setTitle(rs.getString("title"));
        event.setDescription(rs.getString("description"));

        int departmentId = rs.getInt("department_id");
        if (!rs.wasNull()) {
            event.setDepartmentId(departmentId);
        }

        Timestamp eventDateTime = rs.getTimestamp("event_date_time");
        if (eventDateTime != null) {
            event.setEventDateTime(eventDateTime.toLocalDateTime());
        }

        event.setLocation(rs.getString("location"));

        int capacity = rs.getInt("capacity");
        if (!rs.wasNull()) {
            event.setCapacity(capacity);
        }

        int remainingSeats = rs.getInt("remaining_seats");
        if (!rs.wasNull()) {
            event.setRemainingSeats(remainingSeats);
        }

        int categoryId = rs.getInt("category_id");
        if (!rs.wasNull()) {
            event.setCategoryId(categoryId);
        }

        event.setEventType(rs.getString("event_type"));
        event.setImagePath(rs.getString("image_path"));
        event.setStatus(rs.getString("status"));

        int organizerId = rs.getInt("organizer_id");
        if (!rs.wasNull()) {
            event.setOrganizerId(organizerId);
        }

        return event;
    }

    private void validateEventInputs(
            String title,
            String description,
            Integer departmentId,
            LocalDateTime eventDateTime,
            String location,
            Integer capacity,
            Integer categoryId,
            String eventType
    ) {
        if (isBlank(title)) {
            throw new ServiceException("Event title is required.");
        }

        if (isBlank(description)) {
            throw new ServiceException("Description is required.");
        }

        if (departmentId == null || departmentId <= 0) {
            throw new ServiceException("Department is required.");
        }

        if (eventDateTime == null) {
            throw new ServiceException("Event date and time is required.");
        }

        if (isBlank(location)) {
            throw new ServiceException("Location is required.");
        }

        if (capacity == null || capacity <= 0) {
            throw new ServiceException("Capacity must be greater than zero.");
        }

        if (categoryId == null || categoryId <= 0) {
            throw new ServiceException("Category is required.");
        }

        if (!isValidEventType(eventType)) {
            throw new ServiceException("Invalid event type.");
        }
    }

    private boolean isValidEventType(String eventType) {
        return "workshop".equals(eventType)
                || "seminar".equals(eventType)
                || "club_social_event".equals(eventType)
                || "sports_activity".equals(eventType);
    }

    private String cleanText(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
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

        return new ServiceException("Event management operation failed.", e);
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