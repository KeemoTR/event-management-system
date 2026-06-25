package eventsystem.util;

import eventsystem.factory.EventFactory;
import eventsystem.factory.EventFactoryProvider;
import eventsystem.model.Event;

import java.time.LocalDateTime;

public class EventFactoryTest {

    public static void main(String[] args) {

        System.out.println("=== Event Factory Test Started ===");

        EventFactory factory = EventFactoryProvider.getFactory("workshop");

        Event event = factory.createEvent(
                "Factory Java Workshop",
                "Created using Factory Method Pattern",
                1,
                LocalDateTime.now().plusDays(5),
                "Lab 101",
                30,
                1,
                null,
                2
        );

        System.out.println("Title: " + event.getTitle());
        System.out.println("Type: " + event.getEventType());
        System.out.println("Status: " + event.getStatus());
        System.out.println("Capacity: " + event.getCapacity());
        System.out.println("Remaining Seats: " + event.getRemainingSeats());

        System.out.println("=== Event Factory Test Finished ===");
    }
}
