package eventsystem.strategy;

import eventsystem.model.Event;
import java.util.ArrayList;
import java.util.List;

public class TitleSearchStrategy implements EventSearchStrategy {

    private final String title;

    public TitleSearchStrategy(String title) {
        this.title = title;
    }

    @Override
    public List<Event> filter(List<Event> events) {
        List<Event> result = new ArrayList<>();

        if (title == null || title.trim().isEmpty()) {
            return events;
        }

        String searchText = title.trim().toLowerCase();

        for (Event event : events) {
            if (event.getTitle() != null &&
                    event.getTitle().toLowerCase().contains(searchText)) {
                result.add(event);
            }
        }

        return result;
    }
}
