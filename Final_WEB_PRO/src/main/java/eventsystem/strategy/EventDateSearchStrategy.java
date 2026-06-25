package eventsystem.strategy;

import eventsystem.model.Event;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EventDateSearchStrategy implements EventSearchStrategy {

    private final LocalDate date;

    public EventDateSearchStrategy(LocalDate date) {
        this.date = date;
    }

    @Override
    public List<Event> filter(List<Event> events) {
        List<Event> result = new ArrayList<>();

        if (date == null) {
            return events;
        }

        for (Event event : events) {
            if (event.getEventDateTime() != null &&
                    event.getEventDateTime().toLocalDate().equals(date)) {
                result.add(event);
            }
        }

        return result;
    }
}