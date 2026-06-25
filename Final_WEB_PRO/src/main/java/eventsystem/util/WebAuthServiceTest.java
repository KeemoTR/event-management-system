package eventsystem.util;

import eventsystem.model.User;
import eventsystem.service.WebAuthService;

public class WebAuthServiceTest {

    public static void main(String[] args) {

        WebAuthService authService = new WebAuthService();

        String email = "web_auth_test_" + System.currentTimeMillis() + "@test.com";
        String password = "123456";

        try {
            System.out.println("=== REGISTER TEST ===");

            User registeredUser = authService.registerStudent(
                    "Web Auth Test User",
                    email,
                    password,
                    "IT",
                    1,
                    2022
            );

            System.out.println("Registered user ID: " + registeredUser.getId());
            System.out.println("Registered email: " + registeredUser.getEmail());
            System.out.println("Registered role: " + registeredUser.getRole());
            System.out.println("Registered status: " + registeredUser.getStatus());

            if (registeredUser.getPasswordHash() == null) {
                System.out.println("Password hash is hidden from returned User: OK");
            } else {
                System.out.println("Password hash is exposed: PROBLEM");
            }

            System.out.println("\n=== LOGIN TEST: CORRECT PASSWORD ===");

            User loggedInUser = authService.login(email, password);

            System.out.println("Login success.");
            System.out.println("Logged in user ID: " + loggedInUser.getId());
            System.out.println("Logged in email: " + loggedInUser.getEmail());
            System.out.println("Logged in role: " + loggedInUser.getRole());

            if (loggedInUser.getPasswordHash() == null) {
                System.out.println("Password hash is hidden after login: OK");
            } else {
                System.out.println("Password hash is exposed after login: PROBLEM");
            }

            System.out.println("\n=== LOGIN TEST: WRONG PASSWORD ===");

            try {
                authService.login(email, "wrong-password");
                System.out.println("Wrong password login should have failed: PROBLEM");
            } catch (WebAuthService.ServiceException e) {
                System.out.println("Wrong password rejected: OK");
                System.out.println("Message: " + e.getMessage());
            }

            System.out.println("\n=== DUPLICATE EMAIL TEST ===");

            try {
                authService.registerStudent(
                        "Duplicate User",
                        email,
                        password,
                        "IT",
                        1,
                        2022
                );
                System.out.println("Duplicate email should have failed: PROBLEM");
            } catch (WebAuthService.ServiceException e) {
                System.out.println("Duplicate email rejected: OK");
                System.out.println("Message: " + e.getMessage());
            }

            System.out.println("\nWEB AUTH SERVICE TEST PASSED.");

        } catch (Exception e) {
            System.out.println("\nWEB AUTH SERVICE TEST FAILED.");
            e.printStackTrace();
        }
    }
}