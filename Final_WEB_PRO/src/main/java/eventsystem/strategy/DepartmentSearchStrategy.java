package eventsystem.strategy;

import eventsystem.model.Event;
import java.util.ArrayList;
import java.util.List;

public class DepartmentSearchStrategy implements EventSearchStrategy {

    private final int departmentId;

    public DepartmentSearchStrategy(int departmentId) {
        this.departmentId = departmentId;
    }

    @Override
    public List<Event> filter(List<Event> events) {
        List<Event> result = new ArrayList<>();

        for (Event event : events) {
            if (event.getDepartmentId() != null &&
                    event.getDepartmentId() == departmentId) {
                result.add(event);
            }
        }

        return result;
    }
}
