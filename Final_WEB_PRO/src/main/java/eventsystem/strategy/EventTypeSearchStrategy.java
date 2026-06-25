package eventsystem.strategy;

import eventsystem.model.Event;
import java.util.ArrayList;
import java.util.List;

public class EventTypeSearchStrategy implements EventSearchStrategy {

    private final String eventType;

    public EventTypeSearchStrategy(String eventType) {
        this.eventType = eventType;
    }

    @Override
    public List<Event> filter(List<Event> events) {
        List<Event> result = new ArrayList<>();

        if (eventType == null || eventType.trim().isEmpty()) {
            return events;
        }

        String type = eventType.trim().toLowerCase();

        for (Event event : events) {
            if (event.getEventType() != null &&
                    event.getEventType().toLowerCase().equals(type)) {
                result.add(event);
            }
        }

        return result;
    }
}
