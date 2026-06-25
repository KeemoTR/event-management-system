package eventsystem.factory;

public class SeminarEventFactory extends EventFactory {

    @Override
    protected String getEventType() {
        return "seminar";
    }
}
