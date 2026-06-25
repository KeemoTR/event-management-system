package eventsystem.service;

import eventsystem.dao.EventDAO;
import eventsystem.dao.RatingDAO;
import eventsystem.dao.UserDAO;
import eventsystem.model.Event;
import eventsystem.model.Rating;
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

public class RatingService {

    private static final String ROLE_STUDENT = "student";
    private static final String ROLE_ADMIN = "admin";

    private static final String USER_STATUS_ACTIVE = "active";
    private static final String RESERVATION_STATUS_RESERVED = "reserved";

    private final RatingDAO ratingDAO;
    private final UserDAO userDAO;
    private final EventDAO eventDAO;

    public RatingService() {
        this.ratingDAO = new RatingDAO();
        this.userDAO = new UserDAO();
        this.eventDAO = new EventDAO();
    }

    public RatingService(RatingDAO ratingDAO, UserDAO userDAO, EventDAO eventDAO) {
        this.ratingDAO = ratingDAO;
        this.userDAO = userDAO;
        this.eventDAO = eventDAO;
    }

    public Rating addRating(int studentId, int eventId, int ratingValue, String comment) {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                getActiveStudentOrThrow(connection, studentId);

                Event event = getEventOrThrow(connection, eventId);
                validateEventAlreadyStarted(event);

                validateStudentReservedEvent(connection, studentId, eventId);
                validateRatingValue(ratingValue);

                Rating oldRating = getRatingByStudentAndEvent(connection, studentId, eventId);
                if (oldRating != null) {
                    throw new ServiceException("You already rated this event.");
                }

                Rating rating = new Rating();
                rating.setStudentId(studentId);
                rating.setEventId(eventId);
                rating.setRating(ratingValue);
                rating.setComment(cleanComment(comment));

                boolean added = ratingDAO.addRating(rating, connection);
                if (!added) {
                    throw new SQLException("Failed to add rating.");
                }

                Rating createdRating = getRatingByStudentAndEvent(connection, studentId, eventId);

                connection.commit();
                return createdRating;

            } catch (Exception e) {
                safeRollback(connection, e);
                throw wrapAsServiceException("Add rating failed.", e);
            }

        } catch (SQLException e) {
            throw new ServiceException("Database error while adding rating.", e);
        }
    }

    public Rating updateRating(int studentId, int ratingId, int ratingValue, String comment) {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                getActiveStudentOrThrow(connection, studentId);
                validateRatingValue(ratingValue);

                Rating rating = getRatingOrThrow(connection, ratingId);

                if (!rating.getStudentId().equals(studentId)) {
                    throw new ServiceException("Student can only update his own rating.");
                }

                rating.setRating(ratingValue);
                rating.setComment(cleanComment(comment));

                boolean updated = ratingDAO.updateRating(rating, connection);
                if (!updated) {
                    throw new SQLException("Failed to update rating.");
                }

                Rating updatedRating = ratingDAO.getRatingById(connection, ratingId);

                connection.commit();
                return updatedRating;

            } catch (Exception e) {
                safeRollback(connection, e);
                throw wrapAsServiceException("Update rating failed.", e);
            }

        } catch (SQLException e) {
            throw new ServiceException("Database error while updating rating.", e);
        }
    }

    public void deleteRating(int actorId, int ratingId) {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                User actor = getActiveUserOrThrow(connection, actorId);
                Rating rating = getRatingOrThrow(connection, ratingId);

                boolean isAdmin = ROLE_ADMIN.equalsIgnoreCase(actor.getRole());
                boolean isOwnerStudent = ROLE_STUDENT.equalsIgnoreCase(actor.getRole())
                        && actor.getId().equals(rating.getStudentId());

                if (!isAdmin && !isOwnerStudent) {
                    throw new ServiceException("You are not allowed to delete this rating.");
                }

                boolean deleted = ratingDAO.deleteRating(ratingId, connection);
                if (!deleted) {
                    throw new SQLException("Failed to delete rating.");
                }

                connection.commit();

            } catch (Exception e) {
                safeRollback(connection, e);
                throw wrapAsServiceException("Delete rating failed.", e);
            }

        } catch (SQLException e) {
            throw new ServiceException("Database error while deleting rating.", e);
        }
    }

    public Rating getRatingById(int ratingId) {
        Rating rating = ratingDAO.getRatingById(ratingId);

        if (rating == null) {
            throw new ServiceException("Rating not found.");
        }

        return rating;
    }

    public Rating getRatingByStudentAndEvent(int studentId, int eventId) {
        try (Connection connection = DBConnection.getConnection()) {
            return getRatingByStudentAndEvent(connection, studentId, eventId);
        } catch (SQLException e) {
            throw new ServiceException("Database error while loading rating.", e);
        }
    }

    public List<Rating> getRatingsByEvent(int eventId) {
        String sql = "SELECT * FROM ratings WHERE event_id = ? ORDER BY created_at DESC";
        return queryRatings(sql, eventId);
    }

    public List<Rating> getRatingsByStudent(int studentId) {
        String sql = "SELECT * FROM ratings WHERE student_id = ? ORDER BY created_at DESC";
        return queryRatings(sql, studentId);
    }

    public double getAverageRatingForEvent(int eventId) {
        String sql = "SELECT AVG(rating) FROM ratings WHERE event_id = ?";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, eventId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getDouble(1);
                }
            }

        } catch (SQLException e) {
            throw new ServiceException("Database error while calculating average rating.", e);
        }

        return 0.0;
    }

    private User getActiveStudentOrThrow(Connection connection, int studentId) {
        User user = getActiveUserOrThrow(connection, studentId);

        if (!ROLE_STUDENT.equalsIgnoreCase(user.getRole())) {
            throw new ServiceException("Only students can rate events.");
        }

        return user;
    }

    private User getActiveUserOrThrow(Connection connection, int userId) {
        User user = userDAO.getUserById(connection, userId);

        if (user == null) {
            throw new ServiceException("User not found.");
        }

        if (!USER_STATUS_ACTIVE.equalsIgnoreCase(user.getStatus())) {
            throw new ServiceException("Blocked users cannot use rating service.");
        }

        return user;
    }

    private Event getEventOrThrow(Connection connection, int eventId) {
        Event event = eventDAO.getEventById(connection, eventId);

        if (event == null) {
            throw new ServiceException("Event not found.");
        }

        return event;
    }

    private Rating getRatingOrThrow(Connection connection, int ratingId) {
        Rating rating = ratingDAO.getRatingById(connection, ratingId);

        if (rating == null) {
            throw new ServiceException("Rating not found.");
        }

        return rating;
    }

    private void validateEventAlreadyStarted(Event event) {
        if (event.getEventDateTime() == null ||
                event.getEventDateTime().isAfter(LocalDateTime.now())) {
            throw new ServiceException("Student can rate the event after it starts.");
        }
    }

    private void validateStudentReservedEvent(Connection connection, int studentId, int eventId) {
        Reservation reservation = getReservationByStudentAndEvent(connection, studentId, eventId);

        if (reservation == null ||
                !RESERVATION_STATUS_RESERVED.equalsIgnoreCase(reservation.getReservationStatus())) {
            throw new ServiceException("Student must have an active reservation to rate this event.");
        }
    }

    private void validateRatingValue(int ratingValue) {
        if (ratingValue < 1 || ratingValue > 5) {
            throw new ServiceException("Rating must be between 1 and 5.");
        }
    }

    private String cleanComment(String comment) {
        if (comment == null) {
            return null;
        }

        comment = comment.trim();

        if (comment.length() > 500) {
            throw new ServiceException("Comment must be 500 characters or less.");
        }

        return comment;
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
            }

        } catch (SQLException e) {
            throw new ServiceException("Database error while loading reservation.", e);
        }

        return null;
    }

    private Rating getRatingByStudentAndEvent(Connection connection, int studentId, int eventId) {
        String sql = "SELECT * FROM ratings WHERE student_id = ? AND event_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, studentId);
            statement.setInt(2, eventId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRating(resultSet);
                }
            }

        } catch (SQLException e) {
            throw new ServiceException("Database error while loading rating.", e);
        }

        return null;
    }

    private List<Rating> queryRatings(String sql, int value) {
        List<Rating> ratings = new ArrayList<>();

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, value);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ratings.add(mapRating(resultSet));
                }
            }

        } catch (SQLException e) {
            throw new ServiceException("Database error while loading ratings.", e);
        }

        return ratings;
    }

    private Rating mapRating(ResultSet resultSet) throws SQLException {
        Rating rating = new Rating();

        rating.setId(resultSet.getInt("id"));
        rating.setStudentId(resultSet.getInt("student_id"));
        rating.setEventId(resultSet.getInt("event_id"));
        rating.setRating(resultSet.getInt("rating"));
        rating.setComment(resultSet.getString("comment"));

        Timestamp createdAt = resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            rating.setCreatedAt(createdAt.toLocalDateTime());
        }

        return rating;
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

    private void safeRollback(Connection connection, Exception originalException) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            originalException.addSuppressed(rollbackException);
        }
    }

    private ServiceException wrapAsServiceException(String message, Exception e) {
        if (e instanceof ServiceException) {
            return (ServiceException) e;
        }

        return new ServiceException(message, e);
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
