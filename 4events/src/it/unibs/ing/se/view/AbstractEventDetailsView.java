package it.unibs.ing.se.view;

import it.unibs.ing.se.DMO.Connector;
import it.unibs.ing.se.DMO.JsonTranslator;
import it.unibs.ing.se.model.Event;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

abstract public class AbstractEventDetailsView<K> implements PrintableInterface<K> {
    protected JsonTranslator menuTranslation;
    protected JsonTranslator eventTranslation;
    protected Connector dbConnection;
    protected UUID eventID;

    public AbstractEventDetailsView(UUID eventID) {
        dbConnection = Connector.getInstance();
        this.menuTranslation = new JsonTranslator(JsonTranslator.MENU_JSON_PATH);
        this.eventTranslation = new JsonTranslator(JsonTranslator.EVENT_JSON_PATH);
        this.eventID = eventID;
    }

    @Override
    public void print() {
        if (eventID == null) {
            return;
        }

        Event event = null;
        try {
            event = dbConnection.getEvent(eventID);
        } catch (SQLException e) {
            System.err.println(menuTranslation.getTranslation("SQLError"));
            System.exit(1);
        }

        System.out.println(detailedDescription(event));
    }

    @Override
    abstract public K parseInput();

    /**
     * A full event description with all the fields that can be relevant for a user
     * @param event The Event object to describe
     * @return Description string
     */
    private String detailedDescription (Event event) {
        String creatorUsername;
        StringBuilder sb = new StringBuilder();
        sb.append(eventTranslation.getName(event.getEventType())).append('\n');
        try {
            creatorUsername = dbConnection.getUsername(event.getCreatorID());
        } catch (SQLException | IllegalArgumentException e) {
            creatorUsername = "Error fetching username";
        }
        sb.append(eventTranslation.getName("creator")).append(": ").append(creatorUsername).append('\t');
        String eventState = eventTranslation.getTranslation(event.getCurrentState().name());
        sb.append(eventTranslation.getName("state")).append(": ").append(eventState).append('\n');

        Iterator iterator;
        LinkedHashMap<String, Object> setAttributes = event.getNonNullAttributesWithValue(); // Map with all currently valid attributes
        iterator = setAttributes.entrySet().iterator(); // Get an iterator for our map

        while(iterator.hasNext()) {
            Map.Entry entry = (Map.Entry)iterator.next(); // Casts the iterated item to a Map Entry to use it as such
            sb.append(eventTranslation.getName((String) entry.getKey())).append(": ").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }
}
