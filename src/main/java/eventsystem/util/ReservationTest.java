package eventsystem.util;

import eventsystem.dao.*;
import eventsystem.model.*;
import eventsystem.service.ReservationService;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;

public class ReservationTest {

    public static void main(String[] args) {

        ReservationService service = new ReservationService();
        DepartmentDAO departmentDAO = new DepartmentDAO();
        EventCategoryDAO categoryDAO = new EventCategoryDAO();
        UserDAO userDAO = new UserDAO();
        EventDAO eventDAO = new EventDAO();

        try (Connection connection = DBConnection.getConnection()) {

            connection.setAutoCommit(false);

            // =========================
            // 1️⃣ Department
            // =========================
            Department dep = new Department();
            dep.setName("Dep_" + System.currentTimeMillis());
            dep.setUnitType("academic_department");
            departmentDAO.addDepartment(dep, connection);
            dep = departmentDAO.getAllDepartments(connection)
                    .get(departmentDAO.getAllDepartments(connection).size() - 1);

            // =========================
            // 2️⃣ Category
            // =========================
            EventCategory cat = new EventCategory();
            cat.setName("Cat_" + System.currentTimeMillis());
            categoryDAO.addEventCategory(cat, connection);
            cat = categoryDAO.getAllEventCategories(connection)
                    .get(categoryDAO.getAllEventCategories(connection).size() - 1);

            // =========================
            // 3️⃣ Organizer
            // =========================
            User org = new User();
            org.setName("Organizer");
            org.setEmail("org_" + System.currentTimeMillis() + "@test.com");
            org.setPasswordHash("123");
            org.setRole("organizer");
            org.setStatus("active");
            org.setFaculty("IT");
            org.setDepartmentId(dep.getId());
            org.setAdmissionYear(2020);
            userDAO.addUser(org, connection);

            List<User> users = userDAO.getAllUsers(connection);
            org = users.get(users.size() - 1);

            // =========================
            // 4️⃣ Students
            // =========================
            User s1 = new User();
            s1.setName("S1");
            s1.setEmail("s1_" + System.currentTimeMillis() + "@test.com");
            s1.setPasswordHash("123");
            s1.setRole("student");
            s1.setStatus("active");
            s1.setFaculty("IT");
            s1.setDepartmentId(dep.getId());
            s1.setAdmissionYear(2022);
            userDAO.addUser(s1, connection);

            User s2 = new User();
            s2.setName("S2");
            s2.setEmail("s2_" + System.currentTimeMillis() + "@test.com");
            s2.setPasswordHash("123");
            s2.setRole("student");
            s2.setStatus("active");
            s2.setFaculty("IT");
            s2.setDepartmentId(dep.getId());
            s2.setAdmissionYear(2022);
            userDAO.addUser(s2, connection);

            users = userDAO.getAllUsers(connection);
            s1 = users.get(users.size() - 2);
            s2 = users.get(users.size() - 1);

            // =========================
            // 5️⃣ Event (مقعد واحد)
            // =========================
            Event event = new Event();
            event.setTitle("Event_" + System.currentTimeMillis());
            event.setDescription("Test");
            event.setDepartmentId(dep.getId());
            event.setEventDateTime(LocalDateTime.now().plusHours(2));
            event.setLocation("Room");
            event.setCapacity(1);
            event.setRemainingSeats(1);
            event.setCategoryId(cat.getId());
            event.setEventType("workshop");
            event.setStatus("open");
            event.setOrganizerId(org.getId());

            eventDAO.addEvent(event, connection);
            event = eventDAO.getAllEvents(connection)
                    .get(eventDAO.getAllEvents(connection).size() - 1);

            connection.commit();

            System.out.println("=== DATA READY ===");

            final int s1Id = s1.getId();
            final int s2Id = s2.getId();
            final int eventId = event.getId();

            // =========================
            // 6️⃣ التزامن
            // =========================
            Thread t1 = new Thread(() -> {
                try {
                    service.reserveSeat(s1Id, eventId);
                    System.out.println("S1 SUCCESS");
                } catch (Exception e) {
                    System.out.println("S1 FAIL");
                }
            });

            Thread t2 = new Thread(() -> {
                try {
                    service.reserveSeat(s2Id, eventId);
                    System.out.println("S2 SUCCESS");
                } catch (Exception e) {
                    System.out.println("S2 FAIL");
                }
            });

            t1.start();
            t2.start();
            t1.join();
            t2.join();

            // =========================
            // 7️⃣ معرفة مين حجز
            // =========================
            List<Reservation> reservations = service.getReservationsByEvent(eventId);

            int reservedStudentId = reservations.get(0).getStudentId();

            System.out.println("\nReserved by student: " + reservedStudentId);
            System.out.println("Seats: " + service.getRemainingSeats(eventId));

            // =========================
            // 8️⃣ إلغاء (صح 100%)
            // =========================
            System.out.println("\n=== CANCEL ===");

            service.cancelReservation(reservedStudentId, eventId);

            System.out.println("Seats after cancel: " + service.getRemainingSeats(eventId));

            // =========================
            // 9️⃣ إعادة الحجز
            // =========================
            System.out.println("\n=== RE-RESERVE ===");

            try {
                service.reserveSeat(s1Id, eventId);
                System.out.println("Re-reserve SUCCESS");
            } catch (Exception e) {
                System.out.println("Re-reserve FAIL");
            }

            System.out.println("Final seats: " + service.getRemainingSeats(eventId));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}