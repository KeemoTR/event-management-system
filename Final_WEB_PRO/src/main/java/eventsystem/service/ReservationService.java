package eventsystem.service;

import eventsystem.dao.EventDAO;
import eventsystem.dao.ReservationDAO;
import eventsystem.dao.UserDAO;
import eventsystem.model.Event;
import eventsystem.model.Reservation;
import eventsystem.model.User;
import eventsystem.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReservationService {

    private static final String STATUS_RESERVED = "reserved";
    private static final String STATUS_CANCELLED = "cancelled";

    private static final String USER_ROLE_STUDENT = "student";
    private static final String USER_ROLE_ORGANIZER = "organizer";
    private static final String USER_ROLE_ADMIN = "admin";

    private static final String USER_STATUS_ACTIVE = "active";

    private static final String EVENT_STATUS_OPEN = "open";

    private static final String ATTENDANCE_PRESENT = "present";
    private static final String ATTENDANCE_ABSENT = "absent";

    private final ReservationDAO reservationDAO;
    private final EventDAO eventDAO;
    private final UserDAO userDAO;

    public ReservationService() {
        this.reservationDAO = new ReservationDAO();
        this.eventDAO = new EventDAO();
        this.userDAO = new UserDAO();
    }

    public ReservationService(ReservationDAO reservationDAO, EventDAO eventDAO, UserDAO userDAO) {
        this.reservationDAO = reservationDAO;
        this.eventDAO = eventDAO;
        this.userDAO = userDAO;
    }

    public Reservation reserveSeat(int studentId, int eventId) {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                LocalDateTime currentTime = LocalDateTime.now();

                User student = getActiveStudentOrThrow(connection, studentId);
                Event event = getReservableEventOrThrow(connection, eventId, currentTime);

                Reservation existingReservation = getReservationByStudentAndEvent(connection, studentId, eventId);

                if (existingReservation != null
                        && STATUS_RESERVED.equalsIgnoreCase(existingReservation.getReservationStatus())) {
                    throw new ServiceException("You already reserved this event.");
                }

                boolean seatDecreased = decrementSeatAtomically(connection, event.getId(), currentTime);
                if (!seatDecreased) {
                    throw new ServiceException("No seats available for this event.");
                }

                Reservation resultReservation;

                if (existingReservation != null) {
                    boolean reactivated = reactivateCancelledReservationConditionally(connection, studentId, eventId);

                    if (reactivated) {
                        resultReservation = getReservationByStudentAndEvent(connection, studentId, eventId);
                        connection.commit();
                        return resultReservation;
                    }

                    Reservation latest = getReservationByStudentAndEvent(connection, studentId, eventId);
                    if (latest != null && STATUS_RESERVED.equalsIgnoreCase(latest.getReservationStatus())) {
                        throw new ServiceException("You already reserved this event.");
                    }

                    throw new ServiceException("Reservation state changed. Please try again.");
                }

                Reservation reservation = new Reservation();
                reservation.setStudentId(student.getId());
                reservation.setEventId(event.getId());
                reservation.setReservationStatus(STATUS_RESERVED);
                reservation.setAttendanceStatus(null);

                boolean added = reservationDAO.addReservation(reservation, connection);
                if (!added) {
                    throw new SQLException("Failed to create reservation.");
                }

                resultReservation = getReservationByStudentAndEvent(connection, studentId, eventId);
                connection.commit();
                return resultReservation;

            } catch (SQLException e) {
                safeRollback(connection, e);

                if (isDuplicateKey(e)) {
                    throw new ServiceException("You already reserved this event.", e);
                }

                throw wrapAsServiceException("Reservation failed.", e);

            } catch (Exception e) {
                safeRollback(connection, e);
                throw wrapAsServiceException("Reservation failed.", e);
            }

        } catch (SQLException e) {
            throw new ServiceException("Database error while reserving seat.", e);
        }
    }

    public void cancelReservation(int studentId, int eventId) {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                LocalDateTime currentTime = LocalDateTime.now();

                Reservation reservation = getReservationByStudentAndEvent(connection, studentId, eventId);
                if (reservation == null) {
                    throw new ServiceException("Reservation not found.");
                }

                Event event = eventDAO.getEventById(connection, eventId);
                if (event == null) {
                    throw new ServiceException("Event not found.");
                }

                if (hasEventStarted(event, currentTime)) {
                    throw new ServiceException("Reservation cannot be cancelled after the event starts.");
                }

                boolean cancelled = cancelReservationConditionally(connection, studentId, eventId);
                if (!cancelled) {
                    throw new ServiceException("Active reservation not found.");
                }

                boolean seatIncreased = incrementSeatSafely(connection, eventId);
                if (!seatIncreased) {
                    throw new SQLException("Failed to restore event seat.");
                }

                connection.commit();

            } catch (Exception e) {
                safeRollback(connection, e);
                throw wrapAsServiceException("Cancel reservation failed.", e);
            }

        } catch (SQLException e) {
            throw new ServiceException("Database error while cancelling reservation.", e);
        }
    }

    public void markAttendance(int actorId, int reservationId, String attendanceStatus) {
        String normalizedStatus = normalizeAttendanceStatus(attendanceStatus);

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                LocalDateTime currentTime = LocalDateTime.now();

                User actor = userDAO.getUserById(connection, actorId);
                if (actor == null) {
                    throw new ServiceException("User not found.");
                }

                String role = actor.getRole();
                boolean isOrganizer = USER_ROLE_ORGANIZER.equalsIgnoreCase(role);
                boolean isAdmin = USER_ROLE_ADMIN.equalsIgnoreCase(role);

                if (!isOrganizer && !isAdmin) {
                    throw new ServiceException("Only organizer or admin can mark attendance.");
                }

                Reservation reservation = reservationDAO.getReservationById(connection, reservationId);
                if (reservation == null) {
                    throw new ServiceException("Reservation not found.");
                }

                if (!STATUS_RESERVED.equalsIgnoreCase(reservation.getReservationStatus())) {
                    throw new ServiceException("Attendance can only be marked for active reservations.");
                }

                Event event = eventDAO.getEventById(connection, reservation.getEventId());
                if (event == null) {
                    throw new ServiceException("Event not found.");
                }

                if (isOrganizer && !actor.getId().equals(event.getOrganizerId())) {
                    throw new ServiceException("Organizer can only mark attendance for their own events.");
                }

                if (!hasEventStarted(event, currentTime)) {
                    throw new ServiceException("Attendance cannot be marked before the event starts.");
                }

                boolean updated = updateAttendanceConditionally(connection, reservationId, normalizedStatus);
                if (!updated) {
                    throw new ServiceException("Failed to mark attendance.");
                }

                connection.commit();

            } catch (Exception e) {
                safeRollback(connection, e);
                throw wrapAsServiceException("Mark attendance failed.", e);
            }

        } catch (SQLException e) {
            throw new ServiceException("Database error while marking attendance.", e);
        }
    }

    public List<Reservation> getReservationsByStudent(int studentId) {
        String sql = "SELECT * FROM reservations WHERE student_id = ? ORDER BY reserved_at DESC";
        return queryReservations(sql, studentId);
    }

    public List<Reservation> getReservationsByEvent(int eventId) {
        String sql = "SELECT * FROM reservations WHERE event_id = ? ORDER BY reserved_at DESC";
        return queryReservations(sql, eventId);
    }

    public int countActiveReservationsForEvent(int eventId) {
        String sql = "SELECT COUNT(*) FROM reservations WHERE event_id = ? AND reservation_status = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, eventId);
            statement.setString(2, STATUS_RESERVED);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
                return 0;
            }

        } catch (SQLException e) {
            throw new ServiceException("Database error while counting reservations.", e);
        }
    }

    public int getRemainingSeats(int eventId) {
        String sql = "SELECT remaining_seats FROM events WHERE id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, eventId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("remaining_seats");
                }
                throw new ServiceException("Event not found.");
            }

        } catch (SQLException e) {
            throw new ServiceException("Database error while loading remaining seats.", e);
        }
    }

    public Reservation getReservationByStudentAndEvent(int studentId, int eventId) {
        try (Connection connection = DBConnection.getConnection()) {
            return getReservationByStudentAndEvent(connection, studentId, eventId);
        } catch (SQLException e) {
            throw new ServiceException("Database error while loading reservation.", e);
        }
    }

    private User getActiveStudentOrThrow(Connection connection, int studentId) {
        User user = userDAO.getUserById(connection, studentId);

        if (user == null) {
            throw new ServiceException("Student not found.");
        }

        if (!USER_ROLE_STUDENT.equalsIgnoreCase(user.getRole())) {
            throw new ServiceException("Only students can reserve seats.");
        }

        if (!USER_STATUS_ACTIVE.equalsIgnoreCase(user.getStatus())) {
            throw new ServiceException("Blocked users cannot reserve seats.");
        }

        return user;
    }

    private Event getReservableEventOrThrow(Connection connection, int eventId, LocalDateTime currentTime) {
        Event event = eventDAO.getEventById(connection, eventId);

        if (event == null) {
            throw new ServiceException("Event not found.");
        }

        if (!EVENT_STATUS_OPEN.equalsIgnoreCase(event.getStatus())) {
            throw new ServiceException("Event is not open for reservation.");
        }

        if (hasEventStarted(event, currentTime)) {
            throw new ServiceException("Cannot reserve a seat after the event starts.");
        }

        return event;
    }

    private boolean hasEventStarted(Event event, LocalDateTime currentTime) {
        return event.getEventDateTime() != null
                && !event.getEventDateTime().isAfter(currentTime);
    }

    private boolean decrementSeatAtomically(Connection connection, int eventId, LocalDateTime currentTime)
            throws SQLException {

        String sql =
                "UPDATE events " +
                        "SET remaining_seats = remaining_seats - 1 " +
                        "WHERE id = ? " +
                        "AND status = ? " +
                        "AND event_date_time > ? " +
                        "AND remaining_seats > 0";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, eventId);
            statement.setString(2, EVENT_STATUS_OPEN);
            statement.setTimestamp(3, Timestamp.valueOf(currentTime));
            return statement.executeUpdate() == 1;
        }
    }

    private boolean incrementSeatSafely(Connection connection, int eventId) throws SQLException {
        String sql =
                "UPDATE events " +
                        "SET remaining_seats = remaining_seats + 1 " +
                        "WHERE id = ? AND remaining_seats < capacity";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, eventId);
            return statement.executeUpdate() == 1;
        }
    }

    private boolean reactivateCancelledReservationConditionally(Connection connection, int studentId, int eventId)
            throws SQLException {

        String sql =
                "UPDATE reservations " +
                        "SET reservation_status = ?, attendance_status = NULL " +
                        "WHERE student_id = ? AND event_id = ? AND reservation_status = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, STATUS_RESERVED);
            statement.setInt(2, studentId);
            statement.setInt(3, eventId);
            statement.setString(4, STATUS_CANCELLED);
            return statement.executeUpdate() == 1;
        }
    }

    private boolean cancelReservationConditionally(Connection connection, int studentId, int eventId)
            throws SQLException {

        String sql =
                "UPDATE reservations " +
                        "SET reservation_status = ?, attendance_status = NULL " +
                        "WHERE student_id = ? AND event_id = ? AND reservation_status = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, STATUS_CANCELLED);
            statement.setInt(2, studentId);
            statement.setInt(3, eventId);
            statement.setString(4, STATUS_RESERVED);
            return statement.executeUpdate() == 1;
        }
    }

    private boolean updateAttendanceConditionally(Connection connection, int reservationId, String attendanceStatus)
            throws SQLException {

        String sql =
                "UPDATE reservations " +
                        "SET attendance_status = ? " +
                        "WHERE id = ? AND reservation_status = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, attendanceStatus);
            statement.setInt(2, reservationId);
            statement.setString(3, STATUS_RESERVED);
            return statement.executeUpdate() == 1;
        }
    }

    private Reservation getReservationByStudentAndEvent(Connection connection, int studentId, int eventId) {
        String sql = "SELECT * FROM reservations WHERE student_id = ? AND event_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, studentId);
            statement.setInt(2, eventId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapReservation(resultSet);
                }
                return null;
            }

        } catch (SQLException e) {
            throw new ServiceException("Database error while loading reservation.", e);
        }
    }

    private List<Reservation> queryReservations(String sql, int id) {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                List<Reservation> reservations = new ArrayList<>();

                while (resultSet.next()) {
                    reservations.add(mapReservation(resultSet));
                }

                return reservations;
            }

        } catch (SQLException e) {
            throw new ServiceException("Database error while loading reservations.", e);
        }
    }

    private Reservation mapReservation(ResultSet resultSet) throws SQLException {
        Reservation reservation = new Reservation();

        reservation.setId(resultSet.getInt("id"));
        reservation.setStudentId(resultSet.getInt("student_id"));
        reservation.setEventId(resultSet.getInt("event_id"));
        reservation.setReservationStatus(resultSet.getString("reservation_status"));
        reservation.setAttendanceStatus(resultSet.getString("attendance_status"));

        Timestamp reservedAt = resultSet.getTimestamp("reserved_at");
        if (reservedAt != null) {
            reservation.setReservedAt(reservedAt.toLocalDateTime());
        }

        return reservation;
    }

    private String normalizeAttendanceStatus(String attendanceStatus) {
        if (attendanceStatus == null) {
            throw new ServiceException("Attendance status is required.");
        }

        String normalized = attendanceStatus.trim().toLowerCase();

        if (!ATTENDANCE_PRESENT.equals(normalized) && !ATTENDANCE_ABSENT.equals(normalized)) {
            throw new ServiceException("Attendance status must be 'present' or 'absent'.");
        }

        return normalized;
    }

    private boolean isDuplicateKey(SQLException e) {
        String sqlState = e.getSQLState();
        int errorCode = e.getErrorCode();
        return (sqlState != null && sqlState.startsWith("23")) || errorCode == 1062;
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