package it.unibs.ing.se.controller;

import it.unibs.ing.se.DMO.JsonTranslator;
import it.unibs.ing.se.helpers.EventHelper;
import it.unibs.ing.se.view.commands.EventCommand;

import java.util.UUID;

public class PublicEventsController implements ControllerInterface<EventCommand> {
    protected JsonTranslator translation;
    private UUID eventID;
    private UUID userID;

    PublicEventsController(UUID eventID, UUID userID) {
        this.translation = JsonTranslator.getInstance();
        this.eventID = eventID;
        this.userID = userID;
    }

    @Override
    public void perform(EventCommand command) {
        switch (command) {
            case INVALID:
                return;
            case REGISTER:
                EventHelper eHelper = new EventHelper();
                eHelper.register(eventID, userID);
                eHelper.updateStatus(eventID);
        }
    }
}
