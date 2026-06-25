package eventsystem.factory;

import eventsystem.model.Event;

import java.time.LocalDateTime;

public abstract class EventFactory {

    public Event createEvent(
            String title,
            String description,
            Integer departmentId,
            LocalDateTime eventDateTime,
            String location,
            Integer capacity,
            Integer categoryId,
            String imagePath,
            Integer organizerId
    ) {
        Event event = new Event();

        event.setTitle(title);
        event.setDescription(description);
        event.setDepartmentId(departmentId);
        event.setEventDateTime(eventDateTime);
        event.setLocation(location);
        event.setCapacity(capacity);
        event.setRemainingSeats(capacity);
        event.setCategoryId(categoryId);
        event.setEventType(getEventType());
        event.setImagePath(imagePath);
        event.setStatus("open");
        event.setOrganizerId(organizerId);

        return event;
    }

    protected abstract String getEventType();
}
