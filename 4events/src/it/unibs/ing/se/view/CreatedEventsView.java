package it.unibs.ing.se.view;

import it.unibs.ing.se.DMO.Connector;
import it.unibs.ing.se.DMO.JsonTranslator;

import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.UUID;

public class CreatedEventsView extends AbstractEventsView {
    public CreatedEventsView (UUID currentUserID) {
        super(currentUserID);
    }

    @Override
    public void createWorkingSet() {
        dbConnection = Connector.getInstance();
        this.menuTranslation = new JsonTranslator(JsonTranslator.MENU_JSON_PATH);
        try {
            eventIDs = dbConnection.getEventsByCreator(currentUserID);
        } catch (SQLException e) {
            System.err.println(menuTranslation.getTranslation("SQLError"));
            System.exit(1);
        } catch (NoSuchElementException e) {
            eventIDs = null;
        }
    }
}
