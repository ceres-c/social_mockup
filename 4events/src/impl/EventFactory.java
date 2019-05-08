package impl;

import java.util.ArrayList;
import java.util.UUID;

/**
 * A factory class to generate impl.Event subclasses
 */
class EventFactory {
    private Connector myConnector;

    EventFactory (Connector dbConnector) {
        this.myConnector = dbConnector;
    }

    /**
     *
     * @param className A String referring to the first column in categories table of the DB
     * @return  impl.SoccerGame  if provided soccer_game
     *          void        if provided any other string
     * @throws IllegalArgumentException if given className isn't a known type
     */
    Event createEvent (String className,  UUID creatorID) {
        Event returnEvent = null;
        ArrayList<String> catDescription = myConnector.getCategoryDescription(className);

        // Iterate over different classes present in the DB
        if (className.equals(SoccerGame.getCatDBName())) {
            returnEvent = new SoccerGame(creatorID, catDescription.get(0), catDescription.get(1));
        } else {
            throw new IllegalArgumentException("ALERT: unknown event type: " + className);
        }
        return returnEvent;
    }
}
