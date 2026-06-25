package eventsystem.util;

import eventsystem.model.Event;
import eventsystem.model.Reservation;
import eventsystem.model.User;
import eventsystem.service.AuthService;
import eventsystem.service.EventService;
import eventsystem.service.ReservationService;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

public class ProjectIntegrationTest {

    private static final AuthService authService = new AuthService();
    private static final EventService eventService = new EventService();
    private static final ReservationService reservationService = new ReservationService();

    public static void main(String[] args) {
        try {
            SeedData data = resetAndSeed();

            System.out.println("=== 1) AUTH: ROOT ADMIN LOGIN ===");
            User rootAdmin = authService.login(data.rootAdminEmail, "123");
            print(rootAdmin);
            assertTrue(rootAdmin.getId().equals(data.rootAdminId), "root admin login works");

            System.out.println("\n=== 2) AUTH: ADMIN CREATES ORGANIZER ===");
            User organizerInput = new User();
            organizerInput.setName("Organizer One");
            organizerInput.setEmail(uniqueEmail("organizer"));
            organizerInput.setFaculty("IT");
            organizerInput.setDepartmentId(data.departmentId);
            organizerInput.setAdmissionYear(2020);
            organizerInput.setRole("organizer");

            User organizer = authService.createUserByAdmin(data.rootAdminId, organizerInput, "123");
            print(organizer);
            assertTrue("organizer".equalsIgnoreCase(organizer.getRole()), "organizer created by admin");

            System.out.println("\n=== 3) AUTH: ORGANIZER LOGIN ===");
            User organizerLogin = authService.login(organizer.getEmail(), "123");
            print(organizerLogin);
            assertTrue(organizerLogin.getId().equals(organizer.getId()), "organizer login works");

            System.out.println("\n=== 4) AUTH: STUDENT SELF REGISTRATION ===");
            User student1Input = new User();
            student1Input.setName("Student One");
            student1Input.setEmail(uniqueEmail("student1"));
            student1Input.setFaculty("IT");
            student1Input.setDepartmentId(data.departmentId);
            student1Input.setAdmissionYear(2023);

            User student1 = authService.registerStudent(student1Input, "123");
            print(student1);
            assertTrue("student".equalsIgnoreCase(student1.getRole()), "student1 registered");

            User student2Input = new User();
            student2Input.setName("Student Two");
            student2Input.setEmail(uniqueEmail("student2"));
            student2Input.setFaculty("IT");
            student2Input.setDepartmentId(data.departmentId);
            student2Input.setAdmissionYear(2023);

            User student2 = authService.register(student2Input, "123");
            print(student2);
            assertTrue("student".equalsIgnoreCase(student2.getRole()), "student2 registered using register alias");

            User student3Input = new User();
            student3Input.setName("Student Three");
            student3Input.setEmail(uniqueEmail("student3"));
            student3Input.setFaculty("IT");
            student3Input.setDepartmentId(data.departmentId);
            student3Input.setAdmissionYear(2023);

            User student3 = authService.registerStudent(student3Input, "123");
            print(student3);
            assertTrue("student".equalsIgnoreCase(student3.getRole()), "student3 registered");

            System.out.println("\n=== 5) AUTH: EMAIL EXISTS / FIND USER ===");
            assertTrue(authService.emailExists(student1.getEmail()), "emailExists works");
            User foundStudent1 = authService.findUserByEmail(student1.getEmail());
            print(foundStudent1);
            assertTrue(foundStudent1 != null && foundStudent1.getId().equals(student1.getId()), "findUserByEmail works");

            System.out.println("\n=== 6) AUTH: NON-ADMIN CANNOT CREATE ORGANIZER ===");
            User forbiddenOrganizer = new User();
            forbiddenOrganizer.setName("Forbidden Organizer");
            forbiddenOrganizer.setEmail(uniqueEmail("forbidden_org"));
            forbiddenOrganizer.setFaculty("IT");
            forbiddenOrganizer.setDepartmentId(data.departmentId);
            forbiddenOrganizer.setAdmissionYear(2022);
            forbiddenOrganizer.setRole("organizer");

            expectFailure("student cannot create organizer", new ThrowingRunnable() {
                @Override
                public void run() throws Exception {
                    authService.createUserByAdmin(student1.getId(), forbiddenOrganizer, "123");
                }
            });

            System.out.println("\n=== 7) AUTH: BLOCKED USER LOGIN FAIL ===");
            User blockedStudentInput = new User();
            blockedStudentInput.setName("Blocked Student");
            blockedStudentInput.setEmail(uniqueEmail("blocked"));
            blockedStudentInput.setFaculty("IT");
            blockedStudentInput.setDepartmentId(data.departmentId);
            blockedStudentInput.setAdmissionYear(2023);

            User blockedStudent = authService.registerStudent(blockedStudentInput, "123");
            blockUser(blockedStudent.getId());

            expectFailure("blocked user cannot log in", new ThrowingRunnable() {
                @Override
                public void run() throws Exception {
                    authService.login(blockedStudent.getEmail(), "123");
                }
            });

            System.out.println("\n=== 8) EVENT: CREATE EVENT ===");
            Event event = buildEvent(data.departmentId, data.categoryId, "Full Project Workshop", 2, 48);
            Event created = eventService.createEvent(organizer.getId(), event);
            int eventId = created.getId();
            print(created);
            assertTrue(eventId > 0, "event created");

            System.out.println("\n=== 9) EVENT: READ EVENT METHODS ===");
            assertTrue(eventService.getEventById(eventId) != null, "getEventById works");
            assertTrue(!eventService.getAllEvents().isEmpty(), "getAllEvents works");
            assertTrue(!eventService.getOpenEvents().isEmpty(), "getOpenEvents works");
            assertTrue(!eventService.getEventsByOrganizer(organizer.getId()).isEmpty(), "getEventsByOrganizer works");
            assertTrue(!eventService.getEventsByDepartment(data.departmentId).isEmpty(), "getEventsByDepartment works");

            System.out.println("\n=== 10) RESERVATION: BASIC FLOW ===");
            Reservation r1 = reservationService.reserveSeat(student1.getId(), eventId);
            print(r1);
            assertTrue(reservationService.getRemainingSeats(eventId) == 1, "seat count decreased after first reservation");

            Reservation r2 = reservationService.reserveSeat(student2.getId(), eventId);
            print(r2);
            assertTrue(reservationService.getRemainingSeats(eventId) == 0, "seat count decreased after second reservation");

            Reservation foundReservation = reservationService.getReservationByStudentAndEvent(student1.getId(), eventId);
            print(foundReservation);
            assertTrue(foundReservation != null, "getReservationByStudentAndEvent works");

            assertTrue(!reservationService.getReservationsByStudent(student1.getId()).isEmpty(),
                    "getReservationsByStudent works");
            assertTrue(reservationService.getReservationsByEvent(eventId).size() == 2,
                    "getReservationsByEvent works");
            assertTrue(reservationService.countActiveReservationsForEvent(eventId) == 2,
                    "ReservationService countActiveReservationsForEvent works");
            assertTrue(eventService.countActiveReservationsForEvent(eventId) == 2,
                    "EventService countActiveReservationsForEvent works");

            System.out.println("\n=== 11) EVENT: CLOSE / REOPEN REGISTRATION ===");
            Event closed = eventService.closeRegistration(organizer.getId(), eventId);
            print(closed);
            assertTrue("closed".equalsIgnoreCase(closed.getStatus()), "registration closed");

            expectFailure("reserve on closed event should fail", new ThrowingRunnable() {
                @Override
                public void run() throws Exception {
                    reservationService.reserveSeat(student3.getId(), eventId);
                }
            });

            expectFailure("reopen with zero seats should fail", new ThrowingRunnable() {
                @Override
                public void run() throws Exception {
                    eventService.reopenRegistration(rootAdmin.getId(), eventId);
                }
            });

            System.out.println("\n=== 12) RESERVATION: CANCEL / REOPEN / RE-RESERVE ===");
            reservationService.cancelReservation(student2.getId(), eventId);
            assertTrue(reservationService.getRemainingSeats(eventId) == 1, "seat restored after cancellation");

            Event reopened = eventService.reopenRegistration(rootAdmin.getId(), eventId);
            print(reopened);
            assertTrue("open".equalsIgnoreCase(reopened.getStatus()), "registration reopened");

            Reservation r3 = reservationService.reserveSeat(student3.getId(), eventId);
            print(r3);
            assertTrue(reservationService.getRemainingSeats(eventId) == 0, "third student took freed seat");

            System.out.println("\n=== 13) RESERVATION: BLOCKED USER CANNOT RESERVE ===");
            Event secondEvent = eventService.createEvent(
                    organizer.getId(),
                    buildEvent(data.departmentId, data.categoryId, "Blocked User Check", 3, 72)
            );

            expectFailure("blocked student cannot reserve", new ThrowingRunnable() {
                @Override
                public void run() throws Exception {
                    reservationService.reserveSeat(blockedStudent.getId(), secondEvent.getId());
                }
            });

            System.out.println("\n=== 14) EVENT: UPDATE EVENT ===");
            Event updateData = new Event();
            updateData.setId(eventId);
            updateData.setTitle("Full Project Workshop - Updated");
            updateData.setDescription("Updated description");
            updateData.setDepartmentId(data.departmentId);
            updateData.setEventDateTime(LocalDateTime.now().plusDays(3));
            updateData.setLocation("Hall B");
            updateData.setCapacity(5);
            updateData.setCategoryId(data.categoryId);
            updateData.setEventType("seminar");
            updateData.setImagePath("images/updated.png");
            updateData.setStatus("open");

            Event updated = eventService.updateEvent(organizer.getId(), updateData);
            print(updated);
            assertTrue(updated.getCapacity() == 5, "event updated");
            assertTrue(updated.getRemainingSeats() == 3, "remaining seats recalculated correctly");

            System.out.println("\n=== 15) RESERVATION: MARK ATTENDANCE + COMPLETE EVENT ===");
            moveEventToPast(eventId);

            List<Reservation> reservations = reservationService.getReservationsByEvent(eventId);
            assertTrue(reservations.size() == 3, "three reservation rows exist");

            Reservation firstActiveReservation = null;
            Reservation secondActiveReservation = null;

            for (Reservation reservation : reservations) {
                if ("reserved".equalsIgnoreCase(reservation.getReservationStatus())) {
                    if (firstActiveReservation == null) {
                        firstActiveReservation = reservation;
                    } else if (secondActiveReservation == null) {
                        secondActiveReservation = reservation;
                        break;
                    }
                }
            }

            assertTrue(firstActiveReservation != null, "first active reservation found");
            assertTrue(secondActiveReservation != null, "second active reservation found");

            reservationService.markAttendance(organizer.getId(), firstActiveReservation.getId(), "present");
            reservationService.markAttendance(rootAdmin.getId(), secondActiveReservation.getId(), "absent");

            Event completed = eventService.markCompleted(rootAdmin.getId(), eventId);
            print(completed);
            assertTrue("completed".equalsIgnoreCase(completed.getStatus()), "event marked completed");

            System.out.println("\n=== 16) EVENT: DELETE PROTECTION ===");
            expectFailure("cannot delete event with reservations", new ThrowingRunnable() {
                @Override
                public void run() throws Exception {
                    eventService.deleteEvent(rootAdmin.getId(), eventId);
                }
            });

            System.out.println("\n=== 17) EVENT: EXPIRE PAST OPEN EVENTS ===");
            Event expirable = eventService.createEvent(
                    organizer.getId(),
                    buildEvent(data.departmentId, data.categoryId, "Past Open Event", 4, 96)
            );

            moveEventToPastAndKeepOpen(expirable.getId());
            int expiredRows = eventService.expirePastEvents();
            System.out.println("Expired rows: " + expiredRows);

            Event afterExpire = eventService.getEventById(expirable.getId());
            print(afterExpire);
            assertTrue("closed".equalsIgnoreCase(afterExpire.getStatus()), "past open event became closed");

            System.out.println("\n=== 18) EVENT: DELETE CLEAN EVENT ===");
            Event deletable = eventService.createEvent(
                    organizer.getId(),
                    buildEvent(data.departmentId, data.categoryId, "Deletable Event", 10, 120)
            );

            eventService.deleteEvent(rootAdmin.getId(), deletable.getId());

            expectFailure("deleted event should not be found", new ThrowingRunnable() {
                @Override
                public void run() throws Exception {
                    eventService.getEventById(deletable.getId());
                }
            });

            System.out.println("\n=== ALL CURRENT SERVICES INTEGRATION TEST FINISHED SUCCESSFULLY ===");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static SeedData resetAndSeed() throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try (Statement st = connection.createStatement()) {
                st.executeUpdate("DELETE FROM ratings");
                st.executeUpdate("DELETE FROM reservations");
                st.executeUpdate("DELETE FROM events");
                st.executeUpdate("DELETE FROM users");
                st.executeUpdate("DELETE FROM event_categories");
                st.executeUpdate("DELETE FROM departments");

                st.executeUpdate("ALTER TABLE ratings AUTO_INCREMENT = 1");
                st.executeUpdate("ALTER TABLE reservations AUTO_INCREMENT = 1");
                st.executeUpdate("ALTER TABLE events AUTO_INCREMENT = 1");
                st.executeUpdate("ALTER TABLE users AUTO_INCREMENT = 1");
                st.executeUpdate("ALTER TABLE event_categories AUTO_INCREMENT = 1");
                st.executeUpdate("ALTER TABLE departments AUTO_INCREMENT = 1");
            }

            int departmentId = insertDepartment(connection, "Computer Science", "academic_department");
            int categoryId = insertCategory(connection, "Technical");

            String rootAdminEmail = uniqueEmail("rootadmin");
            int rootAdminId = insertUser(
                    connection,
                    "Root Admin",
                    rootAdminEmail,
                    "123",
                    "admin",
                    "active",
                    "IT",
                    departmentId,
                    2019
            );

            connection.commit();

            return new SeedData(departmentId, categoryId, rootAdminId, rootAdminEmail);
        }
    }

    private static Event buildEvent(int departmentId, int categoryId, String title, int capacity, int hoursFromNow) {
        Event event = new Event();
        event.setTitle(title);
        event.setDescription("Integration test event: " + title);
        event.setDepartmentId(departmentId);
        event.setEventDateTime(LocalDateTime.now().plusHours(hoursFromNow));
        event.setLocation("Main Hall");
        event.setCapacity(capacity);
        event.setCategoryId(categoryId);
        event.setEventType("workshop");
        event.setImagePath("images/test.png");
        return event;
    }

    private static void blockUser(int userId) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "UPDATE users SET status = 'blocked' WHERE id = ?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    private static void moveEventToPast(int eventId) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "UPDATE events SET event_date_time = ? WHERE id = ?")) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now().minusHours(2)));
            ps.setInt(2, eventId);
            ps.executeUpdate();
        }
    }

    private static void moveEventToPastAndKeepOpen(int eventId) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "UPDATE events SET event_date_time = ?, status = 'open' WHERE id = ?")) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now().minusDays(1)));
            ps.setInt(2, eventId);
            ps.executeUpdate();
        }
    }

    private static int insertDepartment(Connection connection, String name, String unitType) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO departments (name, unit_type) VALUES (?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, unitType);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static int insertCategory(Connection connection, String name) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO event_categories (name) VALUES (?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
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
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO users (name, email, password_hash, role, status, faculty, department_id, admission_year) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, passwordHash);
            ps.setString(4, role);
            ps.setString(5, status);
            ps.setString(6, faculty);
            ps.setInt(7, departmentId);
            ps.setInt(8, admissionYear);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static void expectFailure(String label, ThrowingRunnable action) {
        try {
            action.run();
            throw new RuntimeException("Expected failure did not happen: " + label);
        } catch (Exception e) {
            System.out.println("EXPECTED FAIL: " + label + " -> " + e.getMessage());
        }
    }

    private static void assertTrue(boolean condition, String label) {
        if (!condition) {
            throw new RuntimeException("ASSERTION FAILED: " + label);
        }
        System.out.println("PASS: " + label);
    }

    private static void print(Object value) {
        System.out.println(value);
    }

    private static String uniqueEmail(String prefix) {
        return prefix + "_" + System.currentTimeMillis() + "@test.com";
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static class SeedData {
        int departmentId;
        int categoryId;
        int rootAdminId;
        String rootAdminEmail;

        SeedData(int departmentId, int categoryId, int rootAdminId, String rootAdminEmail) {
            this.departmentId = departmentId;
            this.categoryId = categoryId;
            this.rootAdminId = rootAdminId;
            this.rootAdminEmail = rootAdminEmail;
        }
    }
}