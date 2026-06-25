package eventsystem.strategy;

import eventsystem.model.Event;
import java.util.ArrayList;
import java.util.List;

public class CategorySearchStrategy implements EventSearchStrategy {

    private final int categoryId;

    public CategorySearchStrategy(int categoryId) {
        this.categoryId = categoryId;
    }

    @Override
    public List<Event> filter(List<Event> events) {
        List<Event> result = new ArrayList<>();

        for (Event event : events) {
            if (event.getCategoryId() != null &&
                    event.getCategoryId() == categoryId) {
                result.add(event);
            }
        }

        return result;
    }
}
