package eventsystem.util;
import eventsystem.dao.UserDAO;
import eventsystem.model.User;

import java.util.List;

public class TestDAO {

    public static void main(String[] args) {
        UserDAO userDAO = new UserDAO();

        String tempEmail = "test_" + System.currentTimeMillis() + "@example.com";

        System.out.println("===== 1) GET ALL BEFORE ADD =====");
        printAllUsers(userDAO);

        System.out.println("\n===== 2) ADD USER =====");
        User newUser = new User();
        newUser.setName("Test User");
        newUser.setEmail(tempEmail);
        newUser.setPasswordHash("123456");
        newUser.setRole("student");
        newUser.setStatus("active");
        newUser.setFaculty("Engineering");
        newUser.setDepartmentId(1); // غيّرها إذا ما عندك department بهذا id
        newUser.setAdmissionYear(2023);

        boolean added = userDAO.addUser(newUser);
        System.out.println("Added: " + added);

        System.out.println("\n===== 3) GET ALL AFTER ADD =====");
        printAllUsers(userDAO);

        User insertedUser = findUserByEmail(userDAO, tempEmail);

        if (insertedUser == null) {
            System.out.println("\nInserted user was not found. Test stopped.");
            return;
        }

        System.out.println("\n===== 4) GET BY ID =====");
        User userById = userDAO.getUserById(insertedUser.getId());
        System.out.println(userById);

        System.out.println("\n===== 5) UPDATE USER =====");
        insertedUser.setName("Updated Test User");
        insertedUser.setFaculty("IT");
        insertedUser.setAdmissionYear(2024);

        boolean updated = userDAO.updateUser(insertedUser);
        System.out.println("Updated: " + updated);

        System.out.println("\n===== 6) GET BY ID AFTER UPDATE =====");
        User updatedUser = userDAO.getUserById(insertedUser.getId());
        System.out.println(updatedUser);

        System.out.println("\n===== 7) DELETE USER =====");
        boolean deleted = userDAO.deleteUser(insertedUser.getId());
        System.out.println("Deleted: " + deleted);

        System.out.println("\n===== 8) GET ALL AFTER DELETE =====");
        printAllUsers(userDAO);
    }

    private static void printAllUsers(UserDAO userDAO) {
        List<User> users = userDAO.getAllUsers();

        if (users.isEmpty()) {
            System.out.println("No users found.");
            return;
        }

        for (User user : users) {
            System.out.println(user);
        }
    }

    private static User findUserByEmail(UserDAO userDAO, String email) {
        List<User> users = userDAO.getAllUsers();

        for (User user : users) {
            if (user.getEmail().equals(email)) {
                return user;
            }
        }

        return null;
    }
}