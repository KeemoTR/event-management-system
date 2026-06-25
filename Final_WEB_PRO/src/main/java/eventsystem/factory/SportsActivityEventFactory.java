package eventsystem.factory;

public class SportsActivityEventFactory extends EventFactory {

    @Override
    protected String getEventType() {
        return "sports_activity";
    }
}
