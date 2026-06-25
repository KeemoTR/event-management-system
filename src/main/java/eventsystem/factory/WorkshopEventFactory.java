package eventsystem.factory;

public class WorkshopEventFactory extends EventFactory {

    @Override
    protected String getEventType() {
        return "workshop";
    }
}
