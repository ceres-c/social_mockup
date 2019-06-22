package it.unibs.ing.se.controller.helpers;

import it.unibs.ing.se.DMO.Connector;
import it.unibs.ing.se.DMO.JsonTranslator;
import it.unibs.ing.se.model.Event;
import it.unibs.ing.se.model.User;
import it.unibs.ing.se.model.fields.OptionalCost;
import it.unibs.ing.se.view.WantedOptionalCostView;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.NoSuchElementException;
import java.util.UUID;

public class EventHelper {
    private Connector dbConnection;
    protected JsonTranslator translation;

    public EventHelper() {
        this.translation = JsonTranslator.getInstance();
        dbConnection = Connector.getInstance();
    }

    public boolean register(UUID eventID, UUID userID) {
        boolean canRegister = false;
        Event event = null;
        User currentUser = null;
        try {
            event = dbConnection.getEvent(eventID);
            currentUser = dbConnection.getUser(userID);
        } catch (SQLException e) {
            System.err.println(translation.getTranslation("SQLError"));
            System.exit(1);
        }

        try {
            canRegister = event.register(currentUser);
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("is already registered")) {
                System.err.println(translation.getTranslation("userAlreadyRegisteredToEvent"));
            } else if (e.getMessage().contains("sex is not allowed")) {
                System.err.println(translation.getTranslation("eventRegistrationSexMismatch"));
            } else if (e.getMessage().contains("age is not allowed")) {
                System.err.println(translation.getTranslation("eventRegistrationAgeMismatch"));
            }
        } catch (IllegalStateException e) {
            System.err.println(translation.getTranslation("eventRegistrationMaximumReached"));
        }

        if (canRegister) {
            WantedOptionalCostView wantedOptionalCostView = new WantedOptionalCostView(eventID);
            wantedOptionalCostView.print();
            LinkedHashMap<String, OptionalCost> selectedCosts = wantedOptionalCostView.parseInput();

            try {
                dbConnection.insertOptionalCosts(selectedCosts, eventID, userID);
                dbConnection.updateEventRegistrations(event);
            } catch (SQLException e) {
                System.err.println(translation.getTranslation("SQLError"));
                System.exit(1);
            }
        }

        return canRegister;
    }

    public void deregister(UUID eventID, UUID userID) {
        Event event = null;
        try {
            event = dbConnection.getEvent(eventID);
        } catch (SQLException e) {
            System.err.println(translation.getTranslation("SQLError"));
            System.exit(1);
        }
        try {
            event.deregister(userID, LocalDateTime.now());
        } catch (IllegalStateException | IllegalArgumentException e) {
            System.err.println(translation.getTranslation("errorEventDeregistration"));
            System.err.println(e.getMessage());
        }
        try {
            dbConnection.updateEventRegistrations(event);
        } catch (SQLException e) {
            System.err.println(translation.getTranslation("SQLError"));
            System.exit(1);
        }
    }

    public void publish(UUID eventID, boolean publishStatus) {
        try {
            dbConnection.updateEventPublished(eventID, publishStatus);
        } catch (SQLException e) {
            System.err.println("FATAL: Impossible to connect to SQL database. Contact your sysadmin");
            System.exit(1);
        }
    }

    public void updateStatus(UUID eventID) {
        Event event = null;
        Event.State oldState = null;
        boolean eventUpdated = false;
        try {
            event = dbConnection.getEvent(eventID);
            oldState = event.getCurrentState();
            eventUpdated = event.updateState(LocalDateTime.now());
            dbConnection.updateEventState(eventID, event.getCurrentState());
        } catch (SQLException e) {
            System.err.println(translation.getTranslation("SQLError"));
            System.exit(1);
        }

        if (eventUpdated &&
                !(oldState == Event.State.CLOSED && event.getCurrentState() == Event.State.OPEN)) {
            // No need to throw notifications if the transition was CLOSED -> OPEN since users already
            // received notifications when the event reached OPEN state first
            NotificationHelper notHelper = new NotificationHelper(eventID);
            notHelper.send();
        }
    }

    /**
     * This method iterates over all the active events in the database and check if any needs to be updated.
     * If so it proceeds to update it and save it back into the database with its updated status.
     * If the new status require to send notifications to user it does so.
     * @throws SQLException If a database access error occurs
     */
    public void updateAllEvents() {
        if (dbConnection == null) throw new IllegalStateException("ALERT: No connection to the database");

        ArrayList<UUID> activeEventsID = null;
        try {
            activeEventsID = dbConnection.getActiveEvents();
        } catch (SQLException e) {
            System.err.println(translation.getTranslation("SQLError"));
            System.exit(1);
        } catch (NoSuchElementException e) {
            // The database is empty
            return;
        }

        for (UUID eventID : activeEventsID) {
            updateStatus(eventID);
        }
    }
}