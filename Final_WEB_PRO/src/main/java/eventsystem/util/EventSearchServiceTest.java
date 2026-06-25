package eventsystem.util;

import eventsystem.model.Event;
import eventsystem.service.EventSearchService;

import java.time.LocalDate;
import java.util.List;

public class EventSearchServiceTest {

    public static void main(String[] args) {

        EventSearchService searchService = new EventSearchService();

        System.out.println("=== Event Search Service Test Started ===");

        // 1) Show all events
        List<Event> allEvents = searchService.getAllEvents();
        System.out.println("All events count: " + allEvents.size());

        // 2) Search by title
        List<Event> titleResults = searchService.searchByTitle("Java");
        System.out.println("Search by title count: " + titleResults.size());
        printEvents(titleResults);

        // 3) Search by department id
        List<Event> departmentResults = searchService.searchByDepartment(1);
        System.out.println("Search by department count: " + departmentResults.size());
        printEvents(departmentResults);

        // 4) Search by category id
        List<Event> categoryResults = searchService.searchByCategory(1);
        System.out.println("Search by category count: " + categoryResults.size());
        printEvents(categoryResults);

        // 5) Search by event type
        List<Event> typeResults = searchService.searchByEventType("workshop");
        System.out.println("Search by event type count: " + typeResults.size());
        printEvents(typeResults);

        // 6) Search available events
        List<Event> availableResults = searchService.searchAvailableEvents();
        System.out.println("Available events count: " + availableResults.size());
        printEvents(availableResults);

        // 7) Advanced search
        List<Event> advancedResults = searchService.advancedSearch(
                "Java",
                1,
                1,
                "workshop",
                LocalDate.of(2026, 4, 1),
                false
        );

        System.out.println("Advanced search count: " + advancedResults.size());
        printEvents(advancedResults);

        System.out.println("=== Event Search Service Test Finished ===");
    }

    private static void printEvents(List<Event> events) {
        for (Event event : events) {
            System.out.println(
                    "ID: " + event.getId()
                            + " | Title: " + event.getTitle()
                            + " | Type: " + event.getEventType()
                            + " | Status: " + event.getStatus()
                            + " | Remaining Seats: " + event.getRemainingSeats()
            );
        }

        System.out.println("--------------------------------");
    }
}
