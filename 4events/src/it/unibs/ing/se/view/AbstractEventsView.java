package it.unibs.ing.se.view;

import it.unibs.ing.se.DMO.Connector;
import it.unibs.ing.se.DMO.JsonTranslator;
import it.unibs.ing.se.model.Event;

import java.sql.SQLException;
import java.util.*;

abstract public class AbstractEventsView implements PrintableInterface<UUID> {
    protected JsonTranslator menuTranslation;
    protected JsonTranslator eventTranslation;
    protected Connector dbConnection;
    protected ArrayList<UUID> eventIDs;
    protected UUID currentUserID;

    public AbstractEventsView(UUID currentUserID) {
        dbConnection = Connector.getInstance();
        this.menuTranslation = new JsonTranslator(JsonTranslator.MENU_JSON_PATH);
        this.eventTranslation = new JsonTranslator(JsonTranslator.EVENT_JSON_PATH);
        this.currentUserID = currentUserID;
    }

    /**
     * This method must initialize eventIDs array with the appropriate event ids
     */
    abstract void createWorkingSet();

    @Override
    public void print() {
        if (eventIDs == null) {
            System.out.println(menuTranslation.getTranslation("noEventsInDB"));
            return;
        }

        Event event = null;
        for (int i = 0; i < eventIDs.size(); i++) {
            try {
                event = dbConnection.getEvent(eventIDs.get(i));
            } catch (SQLException e) {
                System.err.println(menuTranslation.getTranslation("SQLError"));
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

        Integer userSelection = InputManager.inputInteger(menuTranslation.getTranslation("selectEventToShow"), false);
        if (userSelection == null || userSelection - 1 >= eventIDs.size() || userSelection - 1 < 0) {
            System.out.println(menuTranslation.getTranslation("invalidUserSelection"));
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
        sb.append(eventTranslation.getName(event.getEventType())).append('\n');
        sb.append(eventTranslation.getName("title")).append(": ").append(event.getTitle()).append('\n');
        try {
            creatorUsername = dbConnection.getUsername(event.getCreatorID());
        } catch (SQLException | IllegalArgumentException e) {
            creatorUsername = "Error fetching username";
        }
        sb.append(eventTranslation.getName("creator")).append(": ").append(creatorUsername).append('\t');
        String eventState = eventTranslation.getTranslation(event.getCurrentState().name());
        sb.append(eventTranslation.getName("state")).append(": ").append(eventState).append('\n');
        sb.append(eventTranslation.getName("registrationDeadline")).append(": ").append(event.getRegistrationDeadline()).append('\t');
        sb.append(eventTranslation.getName("startDate")).append(": ").append(event.getStartDate()).append('\n');
        return sb.toString();
    }
}
