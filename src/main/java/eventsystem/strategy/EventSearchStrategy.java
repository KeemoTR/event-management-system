package eventsystem.strategy;

import eventsystem.model.Event;
import java.util.List;

public interface EventSearchStrategy {

    List<Event> filter(List<Event> events);
}
