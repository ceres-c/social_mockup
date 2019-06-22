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
    protected JsonTranslator translation;
    protected Connector dbConnection;
    protected UUID eventID;

    public AbstractEventDetailsView(UUID eventID) {
        dbConnection = Connector.getInstance();
        this.translation = JsonTranslator.getInstance();
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
            System.err.println(translation.getTranslation("SQLError"));
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
        sb.append(translation.getName(event.getEventType())).append('\n');
        try {
            creatorUsername = dbConnection.getUsername(event.getCreatorID());
        } catch (SQLException | IllegalArgumentException e) {
            creatorUsername = "Error fetching username";
        }
        sb.append(translation.getName("creator")).append(": ").append(creatorUsername).append('\t');
        String eventState = translation.getTranslation(event.getCurrentState().name());
        sb.append(translation.getName("state")).append(": ").append(eventState).append('\n');

        Iterator iterator;
        LinkedHashMap<String, Object> setAttributes = event.getNonNullAttributesWithValue(); // Map with all currently valid attributes
        iterator = setAttributes.entrySet().iterator(); // Get an iterator for our map

        while(iterator.hasNext()) {
            Map.Entry entry = (Map.Entry)iterator.next(); // Casts the iterated item to a Map Entry to use it as such
            sb.append(translation.getName((String) entry.getKey())).append(": ").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }
}
