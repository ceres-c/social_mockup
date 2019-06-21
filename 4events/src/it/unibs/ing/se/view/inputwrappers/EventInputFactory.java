package it.unibs.ing.se.view.inputwrappers;

public class EventInputFactory {
    /**
     * Instantiates a EventInput object of the current subclass given eventType
     * @param eventType A String referring to the first column in categories table of the DB
     * @return Empty EventInfo Object of the right subclass
     */
    public EventInput createEvent (String eventType) {
        EventInput returnEvent;

        // Iterate over different classes present in the DB
        if (eventType.equals(SoccerGameInput.getClassEventType())) {
            returnEvent = new SoccerGameInput();
        } else if (eventType.equals(MountainHikingInput.getClassEventType())) {
            returnEvent = new MountainHikingInput();
        } else {
            throw new IllegalArgumentException("ALERT: unknown event type: " + eventType);
        }
        return returnEvent;
    }
}
