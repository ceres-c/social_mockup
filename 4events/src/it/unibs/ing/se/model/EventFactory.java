package it.unibs.ing.se.model;

import java.util.UUID;

/**
 * A factory class to generate it.unibs.ing.se.model.Event subclasses
 */
public class EventFactory {
    /**
     * Instantiates a Event object of the current subclass given eventType
     * @param eventType A String referring to the first column in categories table of the DB
     * @param creatorID The UUID of the user who's creating the event
     * @return A Event Object of the right subclass with provided information in place
     * @throws IllegalArgumentException if given eventType isn't a known type
     */
    public Event createEvent (UUID eventID, UUID creatorID, String eventType) {
        Event returnEvent;

        // Iterate over different classes present in the DB
        if (eventType.equals(SoccerGame.getClassEventType())) {
            returnEvent = new SoccerGame(eventID, creatorID);
        } else if (eventType.equals(MountainHiking.getClassEventType())) {
            returnEvent = new MountainHiking(eventID, creatorID);
        } else {
            throw new IllegalArgumentException("ALERT: unknown event type: " + eventType);
        }
        return returnEvent;
    }

    /**
     * This empty constructor has to be used ONLY for dummy objects.
     * If used for Event Objects that will be manipulated, it WILL lead to NullPointerExceptions
     *
     * Here be dragons
     * @param eventType A String referring to the first column in categories table of the DB
     * @return A Event Object of the right subclass with no data
     */
    public Event createEvent (String eventType) {
        Event returnEvent;

        // Iterate over different classes present in the DB
        if (eventType.equals(SoccerGame.getClassEventType())) {
            returnEvent = new SoccerGame();
        } else if (eventType.equals(MountainHiking.getClassEventType())) {
            returnEvent = new MountainHiking();
        } else {
            throw new IllegalArgumentException("ALERT: unknown event type: " + eventType);
        }
        return returnEvent;
    }
}
