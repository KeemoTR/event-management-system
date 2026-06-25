package eventsystem.strategy;

import eventsystem.model.Event;
import java.util.ArrayList;
import java.util.List;

public class AvailabilitySearchStrategy implements EventSearchStrategy {

    @Override
    public List<Event> filter(List<Event> events) {
        List<Event> result = new ArrayList<>();

        for (Event event : events) {
            boolean hasSeats = event.getRemainingSeats() != null
                    && event.getRemainingSeats() > 0;

            boolean isOpen = event.getStatus() != null
                    && event.getStatus().equalsIgnoreCase("open");

            if (hasSeats && isOpen) {
                result.add(event);
            }
        }

        return result;
    }
}
