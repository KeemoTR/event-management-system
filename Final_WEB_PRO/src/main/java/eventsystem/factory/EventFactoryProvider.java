package eventsystem.factory;

public class EventFactoryProvider {

    public static EventFactory getFactory(String eventType) {
        if (eventType == null) {
            throw new IllegalArgumentException("Event type is required.");
        }

        String type = eventType.trim().toLowerCase();

        switch (type) {
            case "workshop":
                return new WorkshopEventFactory();

            case "seminar":
                return new SeminarEventFactory();

            case "club_social_event":
                return new ClubSocialEventFactory();

            case "sports_activity":
                return new SportsActivityEventFactory();

            default:
                throw new IllegalArgumentException("Invalid event type: " + eventType);
        }
    }
}
