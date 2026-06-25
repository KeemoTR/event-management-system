package eventsystem.service;

import eventsystem.dao.DepartmentDAO;
import eventsystem.dao.EventCategoryDAO;
import eventsystem.dao.EventDAO;
import eventsystem.dao.UserDAO;
import eventsystem.model.Event;
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

import eventsystem.factory.EventFactory;
import eventsystem.factory.EventFactoryProvider;



public class EventService {
	
	public Event createEventUsingFactory(
	        int actorId,
	        String eventType,
	        String title,
	        String description,
	        Integer departmentId,
	        LocalDateTime eventDateTime,
	        String location,
	        Integer capacity,
	        Integer categoryId,
	        String imagePath
	) {
	    EventFactory factory = EventFactoryProvider.getFactory(eventType);

	    Event event = factory.createEvent(
	            title,
	            description,
	            departmentId,
	            eventDateTime,
	            location,
	            capacity,
	            categoryId,
	            imagePath,
	            actorId
	    );

	    return createEvent(actorId, event);
	}

    private static final String ROLE_ORGANIZER = "organizer";
    private static final String ROLE_ADMIN = "admin";
    private static final String USER_STATUS_ACTIVE = "active";

    private static final String EVENT_STATUS_OPEN = "open";
    private static final String EVENT_STATUS_CLOSED = "closed";
    private static final String EVENT_STATUS_COMPLETED = "completed";
    private static final String EVENT_STATUS_EXPIRED = "expired";

    private static final String EVENT_TYPE_WORKSHOP = "workshop";
    private static final String EVENT_TYPE_SEMINAR = "seminar";
    private static final String EVENT_TYPE_CLUB_SOCIAL_EVENT = "club_social_event";
    private static final String EVENT_TYPE_SPORTS_ACTIVITY = "sports_activity";

    private final EventDAO eventDAO;
    private final UserDAO userDAO;
    private final DepartmentDAO departmentDAO;
    private final EventCategoryDAO eventCategoryDAO;

    public EventService() {
        this.eventDAO = new EventDAO();
        this.userDAO = new UserDAO();
        this.departmentDAO = new DepartmentDAO();
        this.eventCategoryDAO = new EventCategoryDAO();
    }

    public EventService(EventDAO eventDAO,
                        UserDAO userDAO,
                        DepartmentDAO departmentDAO,
                        EventCategoryDAO eventCategoryDAO) {
        this.eventDAO = eventDAO;
        this.userDAO = userDAO;
        this.departmentDAO = departmentDAO;
        this.eventCategoryDAO = eventCategoryDAO;
    }

    public Event createEvent(int actorId, Event event) {
        if (event == null) {
            throw new ServiceException("Event data is required.");
        }

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                User actor = getActiveActorOrThrow(connection, actorId);
                normalizeAndValidateEventForCreate(connection, actor, event);

                boolean added = eventDAO.addEvent(event, connection);
                if (!added) {
                    throw new SQLException("Failed to create event.");
                }

                Event createdEvent = getLatestCreatedEvent(connection, event.getOrganizerId(), event.getTitle());
                if (createdEvent == null) {
                    throw new SQLException("Event created but could not be loaded.");
                }

                connection.commit();
                return createdEvent;

            } catch (Exception e) {
                safeRollback(connection, e);
                throw wrapAsServiceException("Create event failed.", e);
            }

        } catch (SQLException e) {
            throw new ServiceException("Database error while creating event.", e);
        }
    }

    public Event updateEvent(int actorId, Event updatedEvent) {
        if (updatedEvent == null || updatedEvent.getId() == null) {
            throw new ServiceException("Event id is required.");
        }

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                LocalDateTime currentTime = LocalDateTime.now();

                User actor = getActiveActorOrThrow(connection, actorId);
                Event existingEvent = getEventOrThrow(connection, updatedEvent.getId());
                authorizeActorForEventManagement(actor, existingEvent);
                validateEventNotCompleted(existingEvent);

                normalizeAndValidateEventForUpdate(connection, actor, existingEvent, updatedEvent, currentTime);

                boolean updated = eventDAO.updateEvent(updatedEvent, connection);
                if (!updated) {
                    throw new SQLException("Failed to update event.");
                }

                Event refreshedEvent = eventDAO.getEventById(connection, updatedEvent.getId());
                connection.commit();
                return refreshedEvent;

            } catch (Exception e) {
                safeRollback(connection, e);
                throw wrapAsServiceException("Update event failed.", e);
            }

        } catch (SQLException e) {
            throw new ServiceException("Database error while updating event.", e);
        }
    }

    public void deleteEvent(int actorId, int eventId) {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                User actor = getActiveActorOrThrow(connection, actorId);
                Event event = getEventOrThrow(connection, eventId);
                authorizeActorForEventManagement(actor, event);

                if (hasAnyReservations(connection, eventId)) {
                    throw new ServiceException("Cannot delete event that already has reservations.");
                }

                if (hasAnyRatings(connection, eventId)) {
                    throw new ServiceException("Cannot delete event that already has ratings.");
                }

                boolean deleted = eventDAO.deleteEvent(eventId, connection);
                if (!deleted) {
                    throw new SQLException("Failed to delete event.");
                }

                connection.commit();

            } catch (Exception e) {
                safeRollback(connection, e);
                throw wrapAsServiceException("Delete event failed.", e);
            }

        } catch (SQLException e) {
            throw new ServiceException("Database error while deleting event.", e);
        }
    }

    public Event closeRegistration(int actorId, int eventId) {
        return changeEventStatus(actorId, eventId, EVENT_STATUS_CLOSED, false);
    }

    public Event reopenRegistration(int actorId, int eventId) {
        return changeEventStatus(actorId, eventId, EVENT_STATUS_OPEN, true);
    }

    public Event markCompleted(int actorId, int eventId) {
        return changeEventStatus(actorId, eventId, EVENT_STATUS_COMPLETED, false);
    }

    public int expirePastEvents() {
        String sql = "UPDATE events SET status = ? WHERE status = ? AND event_date_time <= ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, EVENT_STATUS_EXPIRED);
            statement.setString(2, EVENT_STATUS_OPEN);
            statement.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));

            return statement.executeUpdate();

        } catch (SQLException e) {
            throw new ServiceException("Database error while expiring past events.", e);
        }
    }

    public Event getEventById(int eventId) {
        Event event = eventDAO.getEventById(eventId);
        if (event == null) {
            throw new ServiceException("Event not found.");
        }
        return event;
    }

    public List<Event> getAllEvents() {
        return eventDAO.getAllEvents();
    }

    public List<Event> getOpenEvents() {
        String sql = "SELECT * FROM events WHERE status = ? AND event_date_time > ? ORDER BY event_date_time ASC";
        return queryEvents(sql, EVENT_STATUS_OPEN, Timestamp.valueOf(LocalDateTime.now()));
    }

    public List<Event> getEventsByOrganizer(int organizerId) {
        String sql = "SELECT * FROM events WHERE organizer_id = ? ORDER BY event_date_time DESC";
        return queryEvents(sql, organizerId);
    }

    public List<Event> getEventsByDepartment(int departmentId) {
        String sql = "SELECT * FROM events WHERE department_id = ? ORDER BY event_date_time DESC";
        return queryEvents(sql, departmentId);
    }

    public int countActiveReservationsForEvent(int eventId) {
        String sql = "SELECT COUNT(*) FROM reservations WHERE event_id = ? AND reservation_status = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, eventId);
            statement.setString(2, "reserved");

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

    private Event changeEventStatus(int actorId, int eventId, String newStatus, boolean validateSeats) {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                LocalDateTime currentTime = LocalDateTime.now();

                User actor = getActiveActorOrThrow(connection, actorId);
                Event event = getEventOrThrow(connection, eventId);
                authorizeActorForEventManagement(actor, event);

                if (EVENT_STATUS_COMPLETED.equalsIgnoreCase(event.getStatus())) {
                    throw new ServiceException("Completed events cannot be modified.");
                }

                if (EVENT_STATUS_COMPLETED.equalsIgnoreCase(newStatus) && !hasEventStarted(event, currentTime)) {
                    throw new ServiceException("Event cannot be marked completed before it starts.");
                }

                if (EVENT_STATUS_OPEN.equalsIgnoreCase(newStatus)) {
                    if (hasEventStarted(event, currentTime)) {
                        throw new ServiceException("Cannot reopen registration after the event starts.");
                    }
                    if (validateSeats && event.getRemainingSeats() != null && event.getRemainingSeats() <= 0) {
                        throw new ServiceException("Cannot reopen registration when no seats remain.");
                    }
                }

                boolean updated = updateEventStatus(connection, eventId, newStatus);
                if (!updated) {
                    throw new SQLException("Failed to update event status.");
                }

                Event refreshed = eventDAO.getEventById(connection, eventId);
                connection.commit();
                return refreshed;

            } catch (Exception e) {
                safeRollback(connection, e);
                throw wrapAsServiceException("Change event status failed.", e);
            }

        } catch (SQLException e) {
            throw new ServiceException("Database error while changing event status.", e);
        }
    }

    private User getActiveActorOrThrow(Connection connection, int actorId) {
        User actor = userDAO.getUserById(connection, actorId);

        if (actor == null) {
            throw new ServiceException("User not found.");
        }

        if (!USER_STATUS_ACTIVE.equalsIgnoreCase(actor.getStatus())) {
            throw new ServiceException("Blocked users cannot manage events.");
        }

        String role = actor.getRole();
        boolean isOrganizer = ROLE_ORGANIZER.equalsIgnoreCase(role);
        boolean isAdmin = ROLE_ADMIN.equalsIgnoreCase(role);

        if (!isOrganizer && !isAdmin) {
            throw new ServiceException("Only organizer or admin can manage events.");
        }

        return actor;
    }

    private Event getEventOrThrow(Connection connection, int eventId) {
        Event event = eventDAO.getEventById(connection, eventId);
        if (event == null) {
            throw new ServiceException("Event not found.");
        }
        return event;
    }

    private void authorizeActorForEventManagement(User actor, Event event) {
        boolean isAdmin = ROLE_ADMIN.equalsIgnoreCase(actor.getRole());
        boolean isOwnerOrganizer = ROLE_ORGANIZER.equalsIgnoreCase(actor.getRole())
                && actor.getId().equals(event.getOrganizerId());

        if (!isAdmin && !isOwnerOrganizer) {
            throw new ServiceException("Organizer can only manage their own events.");
        }
    }

    private void normalizeAndValidateEventForCreate(Connection connection, User actor, Event event) {
        trimTextFields(event);
        validateRequiredFields(event);
        validateDepartmentAndCategory(connection, event);
        validateEventType(event.getEventType());

        if (event.getEventDateTime() == null || !event.getEventDateTime().isAfter(LocalDateTime.now())) {
            throw new ServiceException("Event date and time must be in the future.");
        }

        if (event.getCapacity() == null || event.getCapacity() <= 0) {
            throw new ServiceException("Capacity must be greater than zero.");
        }

        if (event.getRemainingSeats() == null) {
            event.setRemainingSeats(event.getCapacity());
        }

        if (event.getRemainingSeats() < 0 || event.getRemainingSeats() > event.getCapacity()) {
            throw new ServiceException("Remaining seats must be between 0 and capacity.");
        }

        if (event.getStatus() == null || event.getStatus().trim().isEmpty()) {
            event.setStatus(EVENT_STATUS_OPEN);
        } else {
            event.setStatus(event.getStatus().trim().toLowerCase());
            validateEventStatus(event.getStatus());
        }

        if (EVENT_STATUS_COMPLETED.equalsIgnoreCase(event.getStatus())) {
            throw new ServiceException("New event cannot be created as completed.");
        }

        validateImagePathLength(event.getImagePath());

        if (ROLE_ORGANIZER.equalsIgnoreCase(actor.getRole())) {
            event.setOrganizerId(actor.getId());
        } else {
            validateOrganizer(connection, event.getOrganizerId());
        }
    }

    private void normalizeAndValidateEventForUpdate(Connection connection,
                                                    User actor,
                                                    Event existingEvent,
                                                    Event updatedEvent,
                                                    LocalDateTime currentTime) throws SQLException {

        trimTextFields(updatedEvent);
        validateRequiredFields(updatedEvent);
        validateDepartmentAndCategory(connection, updatedEvent);
        validateEventType(updatedEvent.getEventType());
        validateEventStatus(updatedEvent.getStatus());
        validateImagePathLength(updatedEvent.getImagePath());

        if (updatedEvent.getEventDateTime() == null) {
            throw new ServiceException("Event date and time is required.");
        }

        if (EVENT_STATUS_OPEN.equalsIgnoreCase(updatedEvent.getStatus())
                && !updatedEvent.getEventDateTime().isAfter(currentTime)) {
            throw new ServiceException("Open event must be scheduled in the future.");
        }

        if (ROLE_ORGANIZER.equalsIgnoreCase(actor.getRole())) {
            updatedEvent.setOrganizerId(existingEvent.getOrganizerId());
        } else {
            validateOrganizer(connection, updatedEvent.getOrganizerId());
        }

        int activeReservations = countActiveReservationsForEvent(connection, existingEvent.getId());

        if (updatedEvent.getCapacity() == null || updatedEvent.getCapacity() <= 0) {
            throw new ServiceException("Capacity must be greater than zero.");
        }

        if (updatedEvent.getCapacity() < activeReservations) {
            throw new ServiceException("Capacity cannot be less than active reservations.");
        }

        updatedEvent.setRemainingSeats(updatedEvent.getCapacity() - activeReservations);

        if (EVENT_STATUS_OPEN.equalsIgnoreCase(updatedEvent.getStatus()) && updatedEvent.getRemainingSeats() <= 0) {
            throw new ServiceException("Open event must have at least one available seat.");
        }

        if (EVENT_STATUS_COMPLETED.equalsIgnoreCase(updatedEvent.getStatus())
                && !hasEventStarted(updatedEvent, currentTime)) {
            throw new ServiceException("Event cannot be marked completed before it starts.");
        }
    }

    private void validateEventNotCompleted(Event event) {
        if (EVENT_STATUS_COMPLETED.equalsIgnoreCase(event.getStatus())) {
            throw new ServiceException("Completed events cannot be updated.");
        }
    }

    private void validateRequiredFields(Event event) {
        if (event.getTitle() == null || event.getTitle().trim().isEmpty()) {
            throw new ServiceException("Event title is required.");
        }

        if (event.getDepartmentId() == null) {
            throw new ServiceException("Department is required.");
        }

        if (event.getCategoryId() == null) {
            throw new ServiceException("Category is required.");
        }

        if (event.getLocation() == null || event.getLocation().trim().isEmpty()) {
            throw new ServiceException("Location is required.");
        }
    }

    private void validateDepartmentAndCategory(Connection connection, Event event) {
        if (departmentDAO.getDepartmentById(connection, event.getDepartmentId()) == null) {
            throw new ServiceException("Department not found.");
        }

        if (eventCategoryDAO.getEventCategoryById(connection, event.getCategoryId()) == null) {
            throw new ServiceException("Event category not found.");
        }
    }

    private void validateOrganizer(Connection connection, Integer organizerId) {
        if (organizerId == null) {
            throw new ServiceException("Organizer is required.");
        }

        User organizer = userDAO.getUserById(connection, organizerId);
        if (organizer == null) {
            throw new ServiceException("Organizer not found.");
        }

        if (!ROLE_ORGANIZER.equalsIgnoreCase(organizer.getRole())) {
            throw new ServiceException("Selected user is not an organizer.");
        }

        if (!USER_STATUS_ACTIVE.equalsIgnoreCase(organizer.getStatus())) {
            throw new ServiceException("Organizer account is blocked.");
        }
    }

    private void validateEventType(String eventType) {
        if (eventType == null) {
            throw new ServiceException("Event type is required.");
        }

        String normalized = eventType.trim().toLowerCase();

        boolean valid = EVENT_TYPE_WORKSHOP.equals(normalized)
                || EVENT_TYPE_SEMINAR.equals(normalized)
                || EVENT_TYPE_CLUB_SOCIAL_EVENT.equals(normalized)
                || EVENT_TYPE_SPORTS_ACTIVITY.equals(normalized);

        if (!valid) {
            throw new ServiceException("Invalid event type.");
        }
    }

    private void validateEventStatus(String status) {
        if (status == null) {
            throw new ServiceException("Event status is required.");
        }

        String normalized = status.trim().toLowerCase();

        boolean valid = EVENT_STATUS_OPEN.equals(normalized)
                || EVENT_STATUS_CLOSED.equals(normalized)
                || EVENT_STATUS_COMPLETED.equals(normalized)
                || EVENT_STATUS_EXPIRED.equals(normalized);

        if (!valid) {
            throw new ServiceException("Invalid event status.");
        }
    }

    private void validateImagePathLength(String imagePath) {
        if (imagePath != null && imagePath.length() > 255) {
            throw new ServiceException("Image path is too long.");
        }
    }

    private boolean hasEventStarted(Event event, LocalDateTime currentTime) {
        return event.getEventDateTime() != null && !event.getEventDateTime().isAfter(currentTime);
    }

    private boolean updateEventStatus(Connection connection, int eventId, String newStatus) throws SQLException {
        String sql = "UPDATE events SET status = ? WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, newStatus);
            statement.setInt(2, eventId);
            return statement.executeUpdate() == 1;
        }
    }

    private boolean hasAnyReservations(Connection connection, int eventId) throws SQLException {
        String sql = "SELECT 1 FROM reservations WHERE event_id = ? LIMIT 1";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, eventId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private boolean hasAnyRatings(Connection connection, int eventId) throws SQLException {
        String sql = "SELECT 1 FROM ratings WHERE event_id = ? LIMIT 1";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, eventId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private int countActiveReservationsForEvent(Connection connection, int eventId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM reservations WHERE event_id = ? AND reservation_status = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, eventId);
            statement.setString(2, "reserved");

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
                return 0;
            }
        }
    }

    private Event getLatestCreatedEvent(Connection connection, Integer organizerId, String title) throws SQLException {
        String sql = "SELECT * FROM events WHERE organizer_id = ? AND title = ? ORDER BY id DESC LIMIT 1";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, organizerId);
            statement.setString(2, title);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapEvent(resultSet);
                }
                return null;
            }
        }
    }

    private List<Event> queryEvents(String sql, Object... params) {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            bindParameters(statement, params);

            try (ResultSet resultSet = statement.executeQuery()) {
                List<Event> events = new ArrayList<>();
                while (resultSet.next()) {
                    events.add(mapEvent(resultSet));
                }
                return events;
            }

        } catch (SQLException e) {
            throw new ServiceException("Database error while loading events.", e);
        }
    }

    private void bindParameters(PreparedStatement statement, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object value = params[i];
            int index = i + 1;

            if (value instanceof Integer) {
                statement.setInt(index, (Integer) value);
            } else if (value instanceof String) {
                statement.setString(index, (String) value);
            } else if (value instanceof Timestamp) {
                statement.setTimestamp(index, (Timestamp) value);
            } else {
                statement.setObject(index, value);
            }
        }
    }

    private Event mapEvent(ResultSet resultSet) throws SQLException {
        Event event = new Event();

        event.setId(resultSet.getInt("id"));
        event.setTitle(resultSet.getString("title"));
        event.setDescription(resultSet.getString("description"));
        event.setDepartmentId(resultSet.getInt("department_id"));

        Timestamp eventDateTime = resultSet.getTimestamp("event_date_time");
        if (eventDateTime != null) {
            event.setEventDateTime(eventDateTime.toLocalDateTime());
        }

        event.setLocation(resultSet.getString("location"));
        event.setCapacity(resultSet.getInt("capacity"));
        event.setRemainingSeats(resultSet.getInt("remaining_seats"));
        event.setCategoryId(resultSet.getInt("category_id"));
        event.setEventType(resultSet.getString("event_type"));
        event.setImagePath(resultSet.getString("image_path"));
        event.setStatus(resultSet.getString("status"));
        event.setOrganizerId(resultSet.getInt("organizer_id"));

        Timestamp createdAt = resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            event.setCreatedAt(createdAt.toLocalDateTime());
        }

        return event;
    }

    private void trimTextFields(Event event) {
        if (event.getTitle() != null) {
            event.setTitle(event.getTitle().trim());
        }
        if (event.getDescription() != null) {
            event.setDescription(event.getDescription().trim());
        }
        if (event.getLocation() != null) {
            event.setLocation(event.getLocation().trim());
        }
        if (event.getEventType() != null) {
            event.setEventType(event.getEventType().trim().toLowerCase());
        }
        if (event.getImagePath() != null && event.getImagePath().trim().isEmpty()) {
            event.setImagePath(null);
        } else if (event.getImagePath() != null) {
            event.setImagePath(event.getImagePath().trim());
        }
        if (event.getStatus() != null) {
            event.setStatus(event.getStatus().trim().toLowerCase());
        }
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