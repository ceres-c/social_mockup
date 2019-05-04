package impl;

import java.util.ArrayList;

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
    Event createEvent (String className) {
        Event returnEvent = null;
        ArrayList<String> catDescription = myConnector.getCategoryDescription(className);

        switch (className) { // Iterates over different classes present in the DB
            case "soccer_game":
                returnEvent = new SoccerGame(catDescription.get(0), catDescription.get(1));
                break;
            default:
                throw new IllegalArgumentException("ALERT: unknown event type: " + className);
        }
        return returnEvent;
    }
}
