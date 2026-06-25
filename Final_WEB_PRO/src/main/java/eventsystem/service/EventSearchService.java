package eventsystem.service;

import eventsystem.dao.EventDAO;
import eventsystem.model.Event;
import eventsystem.strategy.AvailabilitySearchStrategy;
import eventsystem.strategy.CategorySearchStrategy;
import eventsystem.strategy.DepartmentSearchStrategy;
import eventsystem.strategy.EventDateSearchStrategy;
import eventsystem.strategy.EventSearchStrategy;
import eventsystem.strategy.EventTypeSearchStrategy;
import eventsystem.strategy.TitleSearchStrategy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EventSearchService {

    private final EventDAO eventDAO;

    public EventSearchService() {
        this.eventDAO = new EventDAO();
    }

    public EventSearchService(EventDAO eventDAO) {
        this.eventDAO = eventDAO;
    }

    public List<Event> getAllEvents() {
        return eventDAO.getAllEvents();
    }

    public List<Event> search(EventSearchStrategy strategy) {
        List<Event> events = eventDAO.getAllEvents();

        if (strategy == null) {
            return events;
        }

        return strategy.filter(events);
    }

    public List<Event> search(EventSearchStrategy... strategies) {
        List<Event> result = new ArrayList<>(eventDAO.getAllEvents());

        if (strategies == null) {
            return result;
        }

        for (EventSearchStrategy strategy : strategies) {
            if (strategy != null) {
                result = strategy.filter(result);
            }
        }

        return result;
    }

    public List<Event> searchByTitle(String title) {
        return search(new TitleSearchStrategy(title));
    }

    public List<Event> searchByDepartment(int departmentId) {
        return search(new DepartmentSearchStrategy(departmentId));
    }

    public List<Event> searchByCategory(int categoryId) {
        return search(new CategorySearchStrategy(categoryId));
    }

    public List<Event> searchByEventType(String eventType) {
        return search(new EventTypeSearchStrategy(eventType));
    }

    public List<Event> searchByDate(LocalDate date) {
        return search(new EventDateSearchStrategy(date));
    }

    public List<Event> searchAvailableEvents() {
        return search(new AvailabilitySearchStrategy());
    }

    public List<Event> advancedSearch(
            String title,
            Integer departmentId,
            Integer categoryId,
            String eventType,
            LocalDate date,
            boolean onlyAvailable
    ) {
        List<EventSearchStrategy> strategies = new ArrayList<>();

        if (title != null && !title.trim().isEmpty()) {
            strategies.add(new TitleSearchStrategy(title));
        }

        if (departmentId != null) {
            strategies.add(new DepartmentSearchStrategy(departmentId));
        }

        if (categoryId != null) {
            strategies.add(new CategorySearchStrategy(categoryId));
        }

        if (eventType != null && !eventType.trim().isEmpty()) {
            strategies.add(new EventTypeSearchStrategy(eventType));
        }

        if (date != null) {
            strategies.add(new EventDateSearchStrategy(date));
        }

        if (onlyAvailable) {
            strategies.add(new AvailabilitySearchStrategy());
        }

        return search(strategies.toArray(new EventSearchStrategy[0]));
    }
}