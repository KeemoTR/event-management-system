package eventsystem.util;

import eventsystem.service.EventExpirationService;

public class EventExpirationServiceTest {

    public static void main(String[] args) {

        System.out.println("=== Event Expiration Service Test Started ===");

        EventExpirationService expirationService = new EventExpirationService();

        try {
            int expiredCount = expirationService.expirePastOpenEvents();

            System.out.println("Expired events count: " + expiredCount);

            System.out.println("=== Event Expiration Service Test Finished ===");

        } catch (Exception e) {
            System.out.println("Test failed");
            e.printStackTrace();
        }
    }
}
