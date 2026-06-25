package eventsystem.util;

import eventsystem.model.Event;
import eventsystem.service.EventService;

import java.time.LocalDateTime;

public class EventServiceFactoryIntegrationTest {

    public static void main(String[] args) {

        System.out.println("=== EventService Factory Integration Test Started ===");

        EventService eventService = new EventService();

        int actorId = 2;

        try {
            Event createdEvent = eventService.createEventUsingFactory(
                    actorId,
                    "sports_activity",
                    "Football Match",
                    "Created through EventService using Factory Method Pattern.",
                    1,
                    LocalDateTime.now().plusDays(10),
                    "University Stadium",
                    22,
                    1,
                    null
            );

            System.out.println("Event created successfully");
            System.out.println("ID: " + createdEvent.getId());
            System.out.println("Title: " + createdEvent.getTitle());
            System.out.println("Type: " + createdEvent.getEventType());
            System.out.println("Status: " + createdEvent.getStatus());
            System.out.println("Capacity: " + createdEvent.getCapacity());
            System.out.println("Remaining Seats: " + createdEvent.getRemainingSeats());
            System.out.println("Organizer ID: " + createdEvent.getOrganizerId());

        } catch (Exception e) {
            System.out.println("Test failed");
            e.printStackTrace();
        }

        System.out.println("=== EventService Factory Integration Test Finished ===");
    }
}
