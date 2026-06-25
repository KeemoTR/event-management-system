package eventsystem.dao;

import eventsystem.model.Event;
import eventsystem.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class EventDAO {

    private static final String EVENT_SELECT_WITH_ORGANIZER =
            "SELECT e.*, u.name AS organizer_name " +
            "FROM events e " +
            "LEFT JOIN users u ON e.organizer_id = u.id ";

    public List<Event> getAllEvents() {
        List<Event> events = new ArrayList<>();

        String sql = EVENT_SELECT_WITH_ORGANIZER + "ORDER BY e.event_date_time";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                events.add(mapEvent(resultSet));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return events;
    }

    public List<Event> getAllEvents(Connection connection) {
        List<Event> events = new ArrayList<>();

        String sql = EVENT_SELECT_WITH_ORGANIZER + "ORDER BY e.event_date_time";

        try (
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                events.add(mapEvent(resultSet));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return events;
    }

    public Event getEventById(int id) {
        String sql = EVENT_SELECT_WITH_ORGANIZER + "WHERE e.id = ?";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapEvent(resultSet);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public Event getEventById(Connection connection, int id) {
        String sql = EVENT_SELECT_WITH_ORGANIZER + "WHERE e.id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapEvent(resultSet);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean addEvent(Event event) {
        String sql =
                "INSERT INTO events " +
                "(title, description, department_id, event_date_time, location, capacity, remaining_seats, category_id, event_type, image_path, status, organizer_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            fillEventStatementForInsertOrUpdate(statement, event);

            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean addEvent(Event event, Connection connection) {
        String sql =
                "INSERT INTO events " +
                "(title, description, department_id, event_date_time, location, capacity, remaining_seats, category_id, event_type, image_path, status, organizer_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            fillEventStatementForInsertOrUpdate(statement, event);

            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean updateEvent(Event event) {
        String sql =
                "UPDATE events " +
                "SET title = ?, description = ?, department_id = ?, event_date_time = ?, location = ?, " +
                "capacity = ?, remaining_seats = ?, category_id = ?, event_type = ?, image_path = ?, status = ?, organizer_id = ? " +
                "WHERE id = ?";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            fillEventStatementForInsertOrUpdate(statement, event);
            statement.setInt(13, event.getId());

            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean updateEvent(Event event, Connection connection) {
        String sql =
                "UPDATE events " +
                "SET title = ?, description = ?, department_id = ?, event_date_time = ?, location = ?, " +
                "capacity = ?, remaining_seats = ?, category_id = ?, event_type = ?, image_path = ?, status = ?, organizer_id = ? " +
                "WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            fillEventStatementForInsertOrUpdate(statement, event);
            statement.setInt(13, event.getId());

            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean deleteEvent(int id) {
        String sql = "DELETE FROM events WHERE id = ?";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, id);

            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean deleteEvent(int id, Connection connection) {
        String sql = "DELETE FROM events WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);

            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
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
        event.setOrganizerName(resultSet.getString("organizer_name"));

        Timestamp createdAt = resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            event.setCreatedAt(createdAt.toLocalDateTime());
        }

        return event;
    }

    private void fillEventStatementForInsertOrUpdate(PreparedStatement statement, Event event)
            throws SQLException {

        statement.setString(1, event.getTitle());
        statement.setString(2, event.getDescription());
        statement.setInt(3, event.getDepartmentId());
        statement.setTimestamp(4, Timestamp.valueOf(event.getEventDateTime()));
        statement.setString(5, event.getLocation());
        statement.setInt(6, event.getCapacity());
        statement.setInt(7, event.getRemainingSeats());
        statement.setInt(8, event.getCategoryId());
        statement.setString(9, event.getEventType());
        statement.setString(10, event.getImagePath());
        statement.setString(11, event.getStatus());
        statement.setInt(12, event.getOrganizerId());
    }
}