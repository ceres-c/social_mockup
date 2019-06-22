package it.unibs.ing.se.view;

import it.unibs.ing.se.DMO.Connector;
import it.unibs.ing.se.DMO.JsonTranslator;
import it.unibs.ing.se.model.Event;

import java.sql.SQLException;
import java.util.*;

abstract public class AbstractEventsView implements PrintableInterface<UUID> {
    protected JsonTranslator translation;
    protected Connector dbConnection;
    protected ArrayList<UUID> eventIDs;

    AbstractEventsView() {
        dbConnection = Connector.getInstance();
        this.translation = JsonTranslator.getInstance();
    }

    /**
     * This method must initialize eventIDs array with the appropriate event ids
     */
    abstract void createWorkingSet();

    @Override
    public void print() {
        if (eventIDs == null) {
            System.out.println(translation.getTranslation("noEventsInDB"));
            return;
        }

        Event event = null;
        for (int i = 0; i < eventIDs.size(); i++) {
            try {
                event = dbConnection.getEvent(eventIDs.get(i));
            } catch (SQLException e) {
                System.err.println(translation.getTranslation("SQLError"));
                System.exit(1);
            }

            System.out.println((i + 1) + ") " + synopsis(event));
        }
    }

    @Override
    public UUID parseInput() {
        if (eventIDs == null) {
            return null;
        }

        Integer userSelection = InputManager.inputInteger(translation.getTranslation("selectEventToShow"), false);
        if (userSelection == null || userSelection - 1 >= eventIDs.size() || userSelection - 1 < 0) {
            System.out.println(translation.getTranslation("invalidUserSelection"));
            return null;
        }

        return eventIDs.get(userSelection - 1);
    }

    /**
     * A short event description with: title, creator's username, registrationDeadline and start date
     * @param event The Event object to describe
     * @return Short description string
     */
    private String synopsis (Event event) {
        String creatorUsername;
        StringBuilder sb = new StringBuilder();
        sb.append(translation.getName(event.getEventType())).append('\n');
        sb.append(translation.getName("title")).append(": ").append(event.getTitle()).append('\n');
        try {
            creatorUsername = dbConnection.getUsername(event.getCreatorID());
        } catch (SQLException | IllegalArgumentException e) {
            creatorUsername = "Error fetching username";
        }
        sb.append(translation.getName("creator")).append(": ").append(creatorUsername).append('\t');
        String eventState = translation.getTranslation(event.getCurrentState().name());
        sb.append(translation.getName("state")).append(": ").append(eventState).append('\n');
        sb.append(translation.getName("registrationDeadline")).append(": ").append(event.getRegistrationDeadline()).append('\t');
        sb.append(translation.getName("startDate")).append(": ").append(event.getStartDate()).append('\n');
        return sb.toString();
    }
}
