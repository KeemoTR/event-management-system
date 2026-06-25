package eventsystem.util;

import eventsystem.factory.EventFactory;
import eventsystem.factory.EventFactoryProvider;
import eventsystem.model.Event;
import eventsystem.service.EventService;

import java.time.LocalDateTime;

public class EventFactoryServiceTest {

    public static void main(String[] args) {

        System.out.println("=== Event Factory + EventService Test Started ===");

        EventService eventService = new EventService();

        int actorId = 2;

        try {
            EventFactory factory = EventFactoryProvider.getFactory("seminar");

            Event event = factory.createEvent(
                    "Factory Seminar Test",
                    "This event was created using Factory Method Pattern.",
                    1,
                    LocalDateTime.now().plusDays(7),
                    "Room 202",
                    40,
                    1,
                    null,
                    actorId
            );

            Event createdEvent = eventService.createEvent(actorId, event);

            System.out.println("Event created successfully");
            System.out.println("ID: " + createdEvent.getId());
            System.out.println("Title: " + createdEvent.getTitle());
            System.out.println("Type: " + createdEvent.getEventType());
            System.out.println("Status: " + createdEvent.getStatus());
            System.out.println("Organizer ID: " + createdEvent.getOrganizerId());

        } catch (Exception e) {
            System.out.println("Test failed");
            e.printStackTrace();
        }

        System.out.println("=== Event Factory + EventService Test Finished ===");
    }
}
