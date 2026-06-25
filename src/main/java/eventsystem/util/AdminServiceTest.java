package eventsystem.util;

import eventsystem.model.User;
import eventsystem.service.AdminService;

import java.util.List;

public class AdminServiceTest {

    public static void main(String[] args) {

        System.out.println("=== Admin Service Test Started ===");

        AdminService adminService = new AdminService();

        int adminId = 1;
        int targetUserId = 3;

        try {
            List<User> users = adminService.getAllUsers(adminId);
            System.out.println("Users count: " + users.size());

            User blockedUser = adminService.blockUser(adminId, targetUserId);
            System.out.println("User blocked successfully");
            System.out.println("User ID: " + blockedUser.getId());
            System.out.println("Status: " + blockedUser.getStatus());

            User unblockedUser = adminService.unblockUser(adminId, targetUserId);
            System.out.println("User unblocked successfully");
            System.out.println("User ID: " + unblockedUser.getId());
            System.out.println("Status: " + unblockedUser.getStatus());

            System.out.println("=== Admin Service Test Finished ===");

        } catch (Exception e) {
            System.out.println("Test failed");
            e.printStackTrace();
        }
    }
}
