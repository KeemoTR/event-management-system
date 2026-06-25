package eventsystem.service;

import eventsystem.dao.EventDAO;
import eventsystem.model.Event;
import eventsystem.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class EventExpirationService {

    private static final String STATUS_OPEN = "open";
    private static final String STATUS_EXPIRED = "expired";

    private final EventDAO eventDAO;

    public EventExpirationService() {
        this.eventDAO = new EventDAO();
    }

    public EventExpirationService(EventDAO eventDAO) {
        this.eventDAO = eventDAO;
    }

    public int expirePastOpenEvents() {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                List<Event> events = eventDAO.getAllEvents(connection);

                int expiredCount = 0;
                LocalDateTime now = LocalDateTime.now();

                for (Event event : events) {
                    boolean isOpen = event.getStatus() != null
                            && event.getStatus().equalsIgnoreCase(STATUS_OPEN);

                    boolean isPast = event.getEventDateTime() != null
                            && event.getEventDateTime().isBefore(now);

                    if (isOpen && isPast) {
                        event.setStatus(STATUS_EXPIRED);

                        boolean updated = eventDAO.updateEvent(event, connection);

                        if (!updated) {
                            throw new ServiceException("Failed to expire event ID: " + event.getId());
                        }

                        expiredCount++;
                    }
                }

                connection.commit();
                return expiredCount;

            } catch (Exception e) {
                rollback(connection, e);
                throw wrap(e);
            }

        } catch (SQLException e) {
            throw new ServiceException("Database error while expiring events.", e);
        }
    }

    public boolean expireEventIfPast(int eventId) {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                Event event = eventDAO.getEventById(connection, eventId);

                if (event == null) {
                    throw new ServiceException("Event not found.");
                }

                boolean isOpen = event.getStatus() != null
                        && event.getStatus().equalsIgnoreCase(STATUS_OPEN);

                boolean isPast = event.getEventDateTime() != null
                        && event.getEventDateTime().isBefore(LocalDateTime.now());

                if (!isOpen || !isPast) {
                    connection.commit();
                    return false;
                }

                event.setStatus(STATUS_EXPIRED);

                boolean updated = eventDAO.updateEvent(event, connection);
                if (!updated) {
                    throw new ServiceException("Failed to expire event.");
                }

                connection.commit();
                return true;

            } catch (Exception e) {
                rollback(connection, e);
                throw wrap(e);
            }

        } catch (SQLException e) {
            throw new ServiceException("Database error while expiring event.", e);
        }
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

        return new ServiceException("Event expiration operation failed.", e);
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
