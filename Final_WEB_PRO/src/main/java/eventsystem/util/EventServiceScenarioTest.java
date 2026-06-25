package eventsystem.util;

import eventsystem.model.Event;
import eventsystem.service.EventService;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

public class EventServiceScenarioTest {

    public static void main(String[] args) {
        EventService service = new EventService();

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                resetAllTables(connection);

                int departmentId = insertDepartment(connection, "Computer Science", "academic_department");
                int categoryId = insertCategory(connection, "Technical");

                int organizerId = insertUser(
                        connection,
                        "Organizer One",
                        uniqueEmail("organizer"),
                        "123",
                        "organizer",
                        "active",
                        "IT",
                        departmentId,
                        2020
                );

                int adminId = insertUser(
                        connection,
                        "Admin One",
                        uniqueEmail("admin"),
                        "123",
                        "admin",
                        "active",
                        "IT",
                        departmentId,
                        2019
                );

                int studentId = insertUser(
                        connection,
                        "Student One",
                        uniqueEmail("student"),
                        "123",
                        "student",
                        "active",
                        "IT",
                        departmentId,
                        2023
                );

                connection.commit();

                System.out.println("=== DATA READY ===");
                System.out.println("Department ID: " + departmentId);
                System.out.println("Category ID  : " + categoryId);
                System.out.println("Organizer ID : " + organizerId);
                System.out.println("Admin ID     : " + adminId);
                System.out.println("Student ID   : " + studentId);

                Event event = new Event();
                event.setTitle("Java Workshop");
                event.setDescription("Intro to Servlets and JDBC");
                event.setDepartmentId(departmentId);
                event.setEventDateTime(LocalDateTime.now().plusDays(2));
                event.setLocation("Lab 301");
                event.setCapacity(5);
                event.setCategoryId(categoryId);
                event.setEventType("workshop");
                event.setImagePath("images/java-workshop.png");

                Event created = service.createEvent(organizerId, event);
                int eventId = created.getId();

                System.out.println("\n=== CREATE EVENT ===");
                System.out.println(created);

                System.out.println("\n=== GET EVENT BY ID ===");
                System.out.println(service.getEventById(eventId));

                System.out.println("\n=== GET ALL EVENTS ===");
                printEvents(service.getAllEvents());

                System.out.println("\n=== GET OPEN EVENTS ===");
                printEvents(service.getOpenEvents());

                System.out.println("\n=== GET EVENTS BY ORGANIZER ===");
                printEvents(service.getEventsByOrganizer(organizerId));

                System.out.println("\n=== GET EVENTS BY DEPARTMENT ===");
                printEvents(service.getEventsByDepartment(departmentId));

                Event updateData = new Event();
                updateData.setId(eventId);
                updateData.setTitle("Advanced Java Workshop");
                updateData.setDescription("Servlets, JDBC, and MVC");
                updateData.setDepartmentId(departmentId);
                updateData.setEventDateTime(LocalDateTime.now().plusDays(3));
                updateData.setLocation("Hall A");
                updateData.setCapacity(8);
                updateData.setCategoryId(categoryId);
                updateData.setEventType("seminar");
                updateData.setImagePath("images/advanced-java.png");
                updateData.setStatus("open");

                Event updated = service.updateEvent(organizerId, updateData);

                System.out.println("\n=== UPDATE EVENT ===");
                System.out.println(updated);

                Event closed = service.closeRegistration(organizerId, eventId);
                System.out.println("\n=== CLOSE REGISTRATION ===");
                System.out.println(closed);

                Event reopened = service.reopenRegistration(adminId, eventId);
                System.out.println("\n=== REOPEN REGISTRATION (BY ADMIN) ===");
                System.out.println(reopened);

                try (Connection c2 = DBConnection.getConnection()) {
                    markEventAsStarted(c2, eventId);
                }

                Event completed = service.markCompleted(adminId, eventId);
                System.out.println("\n=== MARK COMPLETED ===");
                System.out.println(completed);

                Event pastEvent = new Event();
                pastEvent.setTitle("Old Sports Activity");
                pastEvent.setDescription("Already ended activity");
                pastEvent.setDepartmentId(departmentId);
                pastEvent.setEventDateTime(LocalDateTime.now().plusHours(1));
                pastEvent.setLocation("Gym");
                pastEvent.setCapacity(10);
                pastEvent.setCategoryId(categoryId);
                pastEvent.setEventType("sports_activity");
                pastEvent.setImagePath(null);

                Event createdPastEvent = service.createEvent(organizerId, pastEvent);
                try (Connection c3 = DBConnection.getConnection()) {
                    markEventAsPastOpen(c3, createdPastEvent.getId());
                }

                int expiredRows = service.expirePastEvents();
                System.out.println("\n=== EXPIRE PAST EVENTS ===");
                System.out.println("Rows affected: " + expiredRows);
                System.out.println(service.getEventById(createdPastEvent.getId()));

                System.out.println("\n=== DELETE EVENT ===");
                Event deletable = new Event();
                deletable.setTitle("Club Meetup");
                deletable.setDescription("Social event for students");
                deletable.setDepartmentId(departmentId);
                deletable.setEventDateTime(LocalDateTime.now().plusDays(5));
                deletable.setLocation("Club Room");
                deletable.setCapacity(20);
                deletable.setCategoryId(categoryId);
                deletable.setEventType("club_social_event");
                deletable.setImagePath(null);

                Event createdDeletable = service.createEvent(organizerId, deletable);
                service.deleteEvent(adminId, createdDeletable.getId());
                System.out.println("Deleted event id: " + createdDeletable.getId());

                System.out.println("\n=== FINAL EVENTS ===");
                printEvents(service.getAllEvents());

                System.out.println("\nSCENARIO FINISHED SUCCESSFULLY");

            } catch (Exception e) {
                connection.rollback();
                throw e;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void resetAllTables(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM ratings");
            statement.executeUpdate("DELETE FROM reservations");
            statement.executeUpdate("DELETE FROM events");
            statement.executeUpdate("DELETE FROM users");
            statement.executeUpdate("DELETE FROM event_categories");
            statement.executeUpdate("DELETE FROM departments");

            statement.executeUpdate("ALTER TABLE ratings AUTO_INCREMENT = 1");
            statement.executeUpdate("ALTER TABLE reservations AUTO_INCREMENT = 1");
            statement.executeUpdate("ALTER TABLE events AUTO_INCREMENT = 1");
            statement.executeUpdate("ALTER TABLE users AUTO_INCREMENT = 1");
            statement.executeUpdate("ALTER TABLE event_categories AUTO_INCREMENT = 1");
            statement.executeUpdate("ALTER TABLE departments AUTO_INCREMENT = 1");
        }
    }

    private static int insertDepartment(Connection connection, String name, String unitType) throws SQLException {
        String sql = "INSERT INTO departments (name, unit_type) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            statement.setString(2, unitType);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private static int insertCategory(Connection connection, String name) throws SQLException {
        String sql = "INSERT INTO event_categories (name) VALUES (?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private static int insertUser(Connection connection,
                                  String name,
                                  String email,
                                  String passwordHash,
                                  String role,
                                  String status,
                                  String faculty,
                                  int departmentId,
                                  int admissionYear) throws SQLException {
        String sql = "INSERT INTO users (name, email, password_hash, role, status, faculty, department_id, admission_year) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            statement.setString(2, email);
            statement.setString(3, passwordHash);
            statement.setString(4, role);
            statement.setString(5, status);
            statement.setString(6, faculty);
            statement.setInt(7, departmentId);
            statement.setInt(8, admissionYear);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private static void markEventAsStarted(Connection connection, int eventId) throws SQLException {
        String sql = "UPDATE events SET event_date_time = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now().minusHours(1)));
            statement.setInt(2, eventId);
            statement.executeUpdate();
        }
    }

    private static void markEventAsPastOpen(Connection connection, int eventId) throws SQLException {
        String sql = "UPDATE events SET event_date_time = ?, status = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now().minusDays(1)));
            statement.setString(2, "open");
            statement.setInt(3, eventId);
            statement.executeUpdate();
        }
    }

    private static String uniqueEmail(String prefix) {
        return prefix + "_" + System.currentTimeMillis() + "@test.com";
    }

    private static void printEvents(List<Event> events) {
        if (events == null || events.isEmpty()) {
            System.out.println("No events found.");
            return;
        }

        for (Event event : events) {
            System.out.println(event);
        }
    }
}