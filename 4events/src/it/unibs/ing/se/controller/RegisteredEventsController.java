package it.unibs.ing.se.controller;

import it.unibs.ing.se.DMO.Connector;
import it.unibs.ing.se.DMO.JsonTranslator;
import it.unibs.ing.se.controller.helpers.EventHelper;
import it.unibs.ing.se.model.Event;
import it.unibs.ing.se.view.commands.EventCommand;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

public class RegisteredEventsController implements ControllerInterface<EventCommand> {
    protected JsonTranslator menuTranslation;
    private Connector dbConnection;
    private UUID eventID;
    private UUID userID;

    public RegisteredEventsController(UUID eventID, UUID userID) {
        this.menuTranslation = new JsonTranslator(JsonTranslator.MENU_JSON_PATH);
        this.eventID = eventID;
        this.userID = userID;
        try {
            dbConnection = Connector.getInstance();
        } catch (IllegalStateException e) {
            System.err.println(menuTranslation.getTranslation("SQLError"));
            System.exit(1);
        }
    }

    @Override
    public void perform(EventCommand command) {
        switch (command) {
            case INVALID:
                return;
            case DEREGISTER:
                EventHelper eHelper = new EventHelper();
                eHelper.deregister(eventID, userID);
                eHelper.updateStatus(eventID);
                break;
        }
    }
}
