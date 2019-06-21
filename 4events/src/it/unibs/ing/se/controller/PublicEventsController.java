package it.unibs.ing.se.controller;

import it.unibs.ing.se.DMO.Connector;
import it.unibs.ing.se.DMO.JsonTranslator;
import it.unibs.ing.se.controller.helpers.EventHelper;
import it.unibs.ing.se.view.commands.EventCommand;

import java.util.UUID;

public class PublicEventsController implements ControllerInterface<EventCommand> {
    protected JsonTranslator menuTranslation;
    private Connector dbConnection;
    private UUID eventID;
    private UUID userID;

    PublicEventsController(UUID eventID, UUID userID) {
        this.menuTranslation = new JsonTranslator(JsonTranslator.MENU_JSON_PATH);
        this.eventID = eventID;
        this.userID = userID;
        dbConnection = Connector.getInstance();
    }

    @Override
    public void perform(EventCommand command) {
        switch (command) {
            case INVALID:
                return;
            case REGISTER:
                EventHelper eHelper = new EventHelper();
                eHelper.register(eventID, userID);
        }
    }
}
