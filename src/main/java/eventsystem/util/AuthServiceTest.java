package eventsystem.util;

import eventsystem.model.User;
import eventsystem.service.AuthService;

import java.sql.*;
import java.time.Year;

public class AuthServiceTest {

    private static final AuthService authService = new AuthService();

    public static void main(String[] args) {
        try {
            SeedData data = resetAndSeed();

            System.out.println("=== 1) REGISTER STUDENT SUCCESS ===");
            User student = new User();
            student.setName("Student One");
            student.setEmail(uniqueEmail("student"));
            student.setFaculty("IT");
            student.setDepartmentId(data.departmentId);
            student.setAdmissionYear(2023);
            student.setRole("admin"); // intentionally wrong, service should force student

            User createdStudent = authService.registerStudent(student, "123");
            print(createdStudent);
            assertTrue(createdStudent.getId() > 0, "student created");
            assertTrue("student".equalsIgnoreCase(createdStudent.getRole()), "self registration forced role to student");
            assertTrue("active".equalsIgnoreCase(createdStudent.getStatus()), "student status is active");

            System.out.println("\n=== 2) DUPLICATE EMAIL SHOULD FAIL ===");
            User duplicateStudent = new User();
            duplicateStudent.setName("Student Duplicate");
            duplicateStudent.setEmail(createdStudent.getEmail());
            duplicateStudent.setFaculty("IT");
            duplicateStudent.setDepartmentId(data.departmentId);
            duplicateStudent.setAdmissionYear(2023);

            expectFailure("duplicate email", new ThrowingRunnable() {
                @Override
                public void run() {
                    authService.registerStudent(duplicateStudent, "123");
                }
            });

            System.out.println("\n=== 3) LOGIN SUCCESS ===");
            User loggedInStudent = authService.login("   " + createdStudent.getEmail().toUpperCase() + "   ", "123");
            print(loggedInStudent);
            assertTrue(loggedInStudent.getId().equals(createdStudent.getId()), "login returned correct user");

            System.out.println("\n=== 4) LOGIN WRONG PASSWORD SHOULD FAIL ===");
            expectFailure("wrong password", new ThrowingRunnable() {
                @Override
                public void run() {
                    authService.login(createdStudent.getEmail(), "999");
                }
            });

            System.out.println("\n=== 5) EMAIL EXISTS / FIND USER BY EMAIL ===");
            assertTrue(authService.emailExists(createdStudent.getEmail()), "emailExists returns true for existing email");
            assertTrue(!authService.emailExists("not_found_" + System.currentTimeMillis() + "@test.com"),
                    "emailExists returns false for non-existing email");

            User foundUser = authService.findUserByEmail("  " + createdStudent.getEmail().toUpperCase() + " ");
            print(foundUser);
            assertTrue(foundUser != null, "findUserByEmail found user");
            assertTrue(foundUser.getId().equals(createdStudent.getId()), "findUserByEmail returned correct user");

            System.out.println("\n=== 6) BLOCKED USER LOGIN SHOULD FAIL ===");
            blockUser(createdStudent.getId());
            expectFailure("blocked student login", new ThrowingRunnable() {
                @Override
                public void run() {
                    authService.login(createdStudent.getEmail(), "123");
                }
            });

            System.out.println("\n=== 7) ADMIN CREATES ORGANIZER SUCCESS ===");
            User organizer = new User();
            organizer.setName("Organizer One");
            organizer.setEmail(uniqueEmail("organizer"));
            organizer.setFaculty("IT");
            organizer.setDepartmentId(data.departmentId);
            organizer.setAdmissionYear(2020);
            organizer.setRole("organizer");

            User createdOrganizer = authService.createUserByAdmin(data.adminId, organizer, "123");
            print(createdOrganizer);
            assertTrue(createdOrganizer.getId() > 0, "organizer created by admin");
            assertTrue("organizer".equalsIgnoreCase(createdOrganizer.getRole()), "organizer role is correct");

            System.out.println("\n=== 8) ADMIN CREATES ANOTHER ADMIN SUCCESS ===");
            User secondAdmin = new User();
            secondAdmin.setName("Second Admin");
            secondAdmin.setEmail(uniqueEmail("admin"));
            secondAdmin.setFaculty("IT");
            secondAdmin.setDepartmentId(data.departmentId);
            secondAdmin.setAdmissionYear(2019);
            secondAdmin.setRole("admin");

            User createdAdmin = authService.createUserByAdmin(data.adminId, secondAdmin, "123");
            print(createdAdmin);
            assertTrue(createdAdmin.getId() > 0, "admin created by admin");
            assertTrue("admin".equalsIgnoreCase(createdAdmin.getRole()), "admin role is correct");

            System.out.println("\n=== 9) NON-ADMIN CANNOT CREATE PRIVILEGED USER ===");
            User anotherOrganizer = new User();
            anotherOrganizer.setName("Organizer Two");
            anotherOrganizer.setEmail(uniqueEmail("organizer2"));
            anotherOrganizer.setFaculty("IT");
            anotherOrganizer.setDepartmentId(data.departmentId);
            anotherOrganizer.setAdmissionYear(2021);
            anotherOrganizer.setRole("organizer");

            expectFailure("student cannot create organizer", new ThrowingRunnable() {
                @Override
                public void run() {
                    authService.createUserByAdmin(data.nonAdminStudentId, anotherOrganizer, "123");
                }
            });

            System.out.println("\n=== 10) INVALID EMAIL FORMAT SHOULD FAIL ===");
            User invalidEmailUser = new User();
            invalidEmailUser.setName("Bad Email");
            invalidEmailUser.setEmail("bademail");
            invalidEmailUser.setFaculty("IT");
            invalidEmailUser.setDepartmentId(data.departmentId);
            invalidEmailUser.setAdmissionYear(2023);

            expectFailure("invalid email format", new ThrowingRunnable() {
                @Override
                public void run() {
                    authService.registerStudent(invalidEmailUser, "123");
                }
            });

            System.out.println("\n=== 11) INVALID DEPARTMENT SHOULD FAIL ===");
            User invalidDepartmentUser = new User();
            invalidDepartmentUser.setName("Bad Department");
            invalidDepartmentUser.setEmail(uniqueEmail("baddep"));
            invalidDepartmentUser.setFaculty("IT");
            invalidDepartmentUser.setDepartmentId(99999);
            invalidDepartmentUser.setAdmissionYear(2023);

            expectFailure("invalid department", new ThrowingRunnable() {
                @Override
                public void run() {
                    authService.registerStudent(invalidDepartmentUser, "123");
                }
            });

            System.out.println("\n=== 12) INVALID ADMISSION YEAR SHOULD FAIL ===");
            User invalidYearUser = new User();
            invalidYearUser.setName("Bad Year");
            invalidYearUser.setEmail(uniqueEmail("badyear"));
            invalidYearUser.setFaculty("IT");
            invalidYearUser.setDepartmentId(data.departmentId);
            invalidYearUser.setAdmissionYear(Year.now().getValue() + 10);

            expectFailure("invalid admission year", new ThrowingRunnable() {
                @Override
                public void run() {
                    authService.registerStudent(invalidYearUser, "123");
                }
            });

            System.out.println("\n=== 13) SHORT PASSWORD SHOULD FAIL ===");
            User shortPasswordUser = new User();
            shortPasswordUser.setName("Short Password");
            shortPasswordUser.setEmail(uniqueEmail("shortpass"));
            shortPasswordUser.setFaculty("IT");
            shortPasswordUser.setDepartmentId(data.departmentId);
            shortPasswordUser.setAdmissionYear(2023);

            expectFailure("short password", new ThrowingRunnable() {
                @Override
                public void run() {
                    authService.registerStudent(shortPasswordUser, "12");
                }
            });

            System.out.println("\n=== AUTH SERVICE TEST FINISHED SUCCESSFULLY ===");

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

            int adminId = insertUser(
                    connection,
                    "Main Admin",
                    uniqueEmail("mainadmin"),
                    "123", // plain text works with your AuthService compatibility logic
                    "admin",
                    "active",
                    "IT",
                    departmentId,
                    2019
            );

            int nonAdminStudentId = insertUser(
                    connection,
                    "Normal Student",
                    uniqueEmail("normalstudent"),
                    "123",
                    "student",
                    "active",
                    "IT",
                    departmentId,
                    2023
            );

            connection.commit();
            return new SeedData(departmentId, adminId, nonAdminStudentId);
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
                "INSERT INTO users (name, email, password_hash, role, status, faculty, department_id, admission_year) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
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

    private static void blockUser(int userId) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "UPDATE users SET status = 'blocked' WHERE id = ?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    private static void assertTrue(boolean condition, String label) {
        if (!condition) {
            throw new RuntimeException("ASSERTION FAILED: " + label);
        }
        System.out.println("PASS: " + label);
    }

    private static void expectFailure(String label, ThrowingRunnable action) {
        try {
            action.run();
            throw new RuntimeException("Expected failure did not happen: " + label);
        } catch (Exception e) {
            System.out.println("EXPECTED FAIL: " + label + " -> " + e.getMessage());
        }
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
        int adminId;
        int nonAdminStudentId;

        SeedData(int departmentId, int adminId, int nonAdminStudentId) {
            this.departmentId = departmentId;
            this.adminId = adminId;
            this.nonAdminStudentId = nonAdminStudentId;
        }
    }
}