package it.unibs.ing.se.controller;

import it.unibs.ing.se.DMO.Connector;
import it.unibs.ing.se.DMO.JsonTranslator;
import it.unibs.ing.se.model.Event;
import it.unibs.ing.se.model.User;
import it.unibs.ing.se.model.fields.OptionalCost;
import it.unibs.ing.se.view.WantedOptionalCostView;
import it.unibs.ing.se.view.commands.EventCommand;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.UUID;

public class PublicEventsController implements ControllerInterface<EventCommand> {
    protected JsonTranslator menuTranslation;
    private Connector dbConnection;
    private UUID eventID;
    private UUID userID;

    public PublicEventsController(UUID eventID, UUID userID) {
        this.menuTranslation = new JsonTranslator(JsonTranslator.MENU_JSON_PATH);
        this.eventID = eventID;
        this.userID = userID;
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
            case REGISTER:
                boolean canRegister = false;
                Event event = null;
                User currentUser = null;
                try {
                    event = dbConnection.getEvent(eventID);
                    currentUser = dbConnection.getUser(userID);
                } catch (SQLException e) {
                    System.err.println(menuTranslation.getTranslation("SQLError"));
                    System.exit(1);
                }

                try {
                    canRegister = event.register(currentUser);
                } catch (IllegalArgumentException e) {
                    if (e.getMessage().contains("is already registered")) {
                        System.err.println(menuTranslation.getTranslation("userAlreadyRegisteredToEvent"));
                    } else if (e.getMessage().contains("sex is not allowed")) {
                        System.err.println(menuTranslation.getTranslation("eventRegistrationSexMismatch"));
                    } else if (e.getMessage().contains("age is not allowed")) {
                        System.err.println(menuTranslation.getTranslation("eventRegistrationAgeMismatch"));
                    }
                } catch (IllegalStateException e) {
                    System.err.println(menuTranslation.getTranslation("eventRegistrationMaximumReached"));
                }

                if (canRegister) {
                    WantedOptionalCostView wantedOptionalCostView = new WantedOptionalCostView(eventID);
                    wantedOptionalCostView.print();
                    LinkedHashMap<String, OptionalCost> selectedCosts = wantedOptionalCostView.parseInput();

                    try {
                        dbConnection.insertOptionalCosts(selectedCosts, eventID, userID);
                        dbConnection.updateEventRegistrations(event);
                    } catch (SQLException e) {
                        System.err.println(menuTranslation.getTranslation("SQLError"));
                        System.exit(1);
                    }
                }
                break;
        }
    }
}
