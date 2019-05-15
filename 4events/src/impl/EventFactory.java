package impl;

import java.util.UUID;

/**
 * A factory class to generate impl.Event subclasses
 */
class EventFactory {
    /**
     *
     * @param eventType A String referring to the first column in categories table of the DB
     * @param creatorID The UUID of the user who's creating the event
     * @return  impl.SoccerGame  if provided soccer_game
     *          void        if provided any other string
     * @throws IllegalArgumentException if given eventType isn't a known type
     */
    Event createEvent (UUID eventID, UUID creatorID, String eventType) {
        Event returnEvent = null;

        // Iterate over different classes present in the DB
        if (eventType.equals(SoccerGame.getClassEventTypeDB())) {
            returnEvent = new SoccerGame(eventID, creatorID);
        } else {
            throw new IllegalArgumentException("ALERT: unknown event type: " + eventType);
        }
        return returnEvent;
    }
}
