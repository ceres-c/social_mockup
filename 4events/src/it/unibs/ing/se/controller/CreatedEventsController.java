package it.unibs.ing.se.controller;

import it.unibs.ing.se.DMO.Connector;
import it.unibs.ing.se.controller.helpers.EventHelper;
import it.unibs.ing.se.model.Event;
import it.unibs.ing.se.view.commands.EventCommand;

import java.sql.SQLException;
import java.util.UUID;

public class CreatedEventsController implements ControllerInterface<EventCommand> {
    private Connector dbConnection;
    private UUID eventID;

    public CreatedEventsController(UUID eventID) {
        this.eventID = eventID;
        try {
            dbConnection = Connector.getInstance();
        } catch (IllegalStateException e) {
            System.err.println("FATAL: Impossible to connect to SQL database. Contact your sysadmin");
            System.exit(1);
        }
    }

    @Override
    public void perform(EventCommand command) {
        switch (command) {
            case INVALID:
                return;
            case PUBLISH:
                EventHelper eHelper = new EventHelper();
                eHelper.publish(eventID, true);
                break;
            case WITHDRAW:
                try {
                    dbConnection.updateEventState(eventID, Event.State.WITHDRAWN);
                } catch (SQLException e) {
                    System.err.println("FATAL: Impossible to connect to SQL database. Contact your sysadmin");
                    System.exit(1);
                }
                break;
        }
    }
}
