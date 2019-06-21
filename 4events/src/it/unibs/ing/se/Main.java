package it.unibs.ing.se;

import it.unibs.ing.se.DMO.Connector;
import it.unibs.ing.se.DMO.JsonConfigReader;
import it.unibs.ing.se.DMO.JsonTranslator;
import it.unibs.ing.se.controller.MainMenuController;
import it.unibs.ing.se.view.*;
import it.unibs.ing.se.view.commands.MainCommand;
import it.unibs.ing.se.model.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

public class Main {
    private static final String CONFIG_JSON_PATH = "config.json";

    public static void main(String[] args) {
        Path configJsonPath = Paths.get(CONFIG_JSON_PATH);
        JsonConfigReader config = new JsonConfigReader(configJsonPath.toString());

        JsonTranslator menuTranslation = new JsonTranslator(JsonTranslator.MENU_JSON_PATH);

        // Initializes the Connector for future usage
        try {
            Connector.getInstance(config.getDBURL(), config.getDBUser(), config.getDBPassword());
        } catch (SQLException e) {
            System.out.println(menuTranslation.getTranslation("SQLError"));
            e.printStackTrace();
            System.exit(1);
        }

        /*
        try { // TODO muovere altrove
            updateAllEvents(dbConnection, eventTranslation, currentDateTime);
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }

        Menu menu = Menu.getInstance(dbConnection, menuTranslation); // TODO remove this
         */

        System.out.println(menuTranslation.getTranslation("welcome"));

        MainMenuController mainController = new MainMenuController();
        mainController.loginAndSet();

        UUID currentUserID = mainController.getCurrentUserID();
        MainMenuView mainView = new MainMenuView(currentUserID);
        MainCommand userSelection;

        while (true) {
            mainView.print();
            userSelection = mainView.parseInput();
            mainController.perform(userSelection);

            /*
            switch (userSelection) {
                case INVALID:
                    break;
                case DASHBOARD:
                    DashboardCommand dashboardUserSelection = it.unibs.ing.se.view.displayDashboard();
                    switch (dashboardUserSelection) {
                        case USER_PROFILE:
                            User newUser = it.unibs.ing.se.view.displayAndEditUserProfile(currentUser);
                            if (!newUser.equals(currentUser)) { // User made some changes to its profile
                                try {
                                    dbConnection.updateUser(newUser); // Then Update the database
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                    System.exit(1);
                                }
                                currentUser = newUser; // And refresh currentUser
                            }
                            break;
                        case PERSONAL_NOTIFICATIONS:
                            it.unibs.ing.se.view.displayNotifications(currentUser);
                            break;
                        case CREATED_EVENTS:
                            Event existingEvent = it.unibs.ing.se.view.displayAndSelectCreatedEvents(currentUser);
                            if (existingEvent != null) {
                                // User want to publish an event which wasn't published before
                                System.out.println(existingEvent.detailedDescription(eventTranslation, dbConnection));

                                if (it.unibs.ing.se.view.publishEvent()) {
                                    // The user wants to publish the event
                                    try {
                                        existingEvent.publish(currentDateTime);
                                        existingEvent.updateState(currentDateTime); // VALID -> OPEN
                                        try {
                                            dbConnection.updateEventState(existingEvent);
                                            dbConnection.updateEventPublished(existingEvent);
                                        } catch (SQLException e) {
                                            e.printStackTrace();
                                            System.exit(1);
                                        }
                                    } catch (IllegalStateException e) {
                                        System.err.println(e.getMessage());
                                        break;
                                    }
                                } else if (it.unibs.ing.se.view.withdrawEvent()) {
                                    // The user wants to withdraw the event
                                    existingEvent.setEventWithdrawn();
                                    try {
                                        dbConnection.updateEventState(existingEvent);
                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                        System.exit(1);
                                    }

                                    // Generate notifications
                                    ArrayList<UUID> registeredUsers = existingEvent.getRegisteredUsers();
                                    for (UUID recipientID : registeredUsers) {
                                        try {
                                            Notification newNotification = Notification.withdrawnEventNotification(existingEvent, eventTranslation, recipientID, dbConnection.getUsername(recipientID));
                                            dbConnection.insertNotification(newNotification);
                                        } catch (SQLException e) {
                                            e.printStackTrace();
                                            System.exit(1);
                                        }
                                    }
                                }
                            }
                            break;
                        case REGISTERED_EVENTS:
                            it.unibs.ing.se.view.displayEventsByRegistration(currentUser, currentDateTime);
                            break;
                        default:
                            System.out.println(menuTranslation.getTranslation("invalidUserSelection"));
                            break;
                    }
                    break;
                case PUBLIC_EVENTS_LIST: {
                    Event existingEvent;
                    existingEvent = it.unibs.ing.se.view.chooseEventFromPublicList();

                    if (existingEvent != null) {
                        // Means the user has selected an event from the list
                        System.out.println(existingEvent.detailedDescription(eventTranslation, dbConnection));

                        if (it.unibs.ing.se.view.registerEvent()) {
                            // The user wants to register
                            boolean canRegister = false;
                            try {
                                canRegister = existingEvent.register(currentUser);
                            } catch (IllegalArgumentException ex) {
                                if (ex.getMessage().contains("is already registered")) {
                                    System.err.println(menuTranslation.getTranslation("userAlreadyRegisteredToEvent"));
                                } else if (ex.getMessage().contains("sex is not allowed")) {
                                    System.err.println(menuTranslation.getTranslation("eventRegistrationSexMismatch"));
                                } else if (ex.getMessage().contains("age is not allowed")) {
                                    System.err.println(menuTranslation.getTranslation("eventRegistrationAgeMismatch"));
                                }
                            } catch (IllegalStateException e) {
                                System.err.println(menuTranslation.getTranslation("eventRegistrationMaximumReached"));
                            }

                            if (canRegister) {
                                try {
                                    dbConnection.insertOptionalCosts(it.unibs.ing.se.view.wantedOptionalCosts(existingEvent.getOptionalCosts()), existingEvent.getEventID(), currentUser.getUserID());
                                    // If this kind of event has optional costs it prompts the user to choose which one he wants
                                    // and saves them in the database
                                    dbConnection.updateEventRegistrations(existingEvent);
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                    System.exit(1);
                                }
                            }

                            if (existingEvent.updateState(currentDateTime)) {
                                // This registration has brought the number of registered users to the wanted participants number
                                try {
                                    dbConnection.updateEventState(existingEvent);
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                    System.exit(1);
                                }
                                // We have to fire off notifications
                                try {
                                    ArrayList<UUID> registeredUsers = existingEvent.getRegisteredUsers();
                                    double eventCost;
                                    if (! registeredUsers.contains(existingEvent.getCreatorID())) {
                                        ArrayList<UUID> creatorCosts = dbConnection.getOptionalCosts(existingEvent.getCreatorID(), existingEvent.getEventID());
                                        eventCost = existingEvent.totalCost(creatorCosts);
                                        // Probably the creator could not join the even due to a sex mismatch, but it has to be informed as well
                                        Notification newNotification = Notification.closedEventNotification(existingEvent, eventTranslation, existingEvent.getCreatorID(), dbConnection.getUsername(existingEvent.getCreatorID()), eventCost);
                                        dbConnection.insertNotification(newNotification);
                                    }
                                    for (UUID recipientID : registeredUsers) {
                                        ArrayList<UUID> userCosts = dbConnection.getOptionalCosts(recipientID, existingEvent.getEventID());
                                        eventCost = existingEvent.totalCost(userCosts);
                                        Notification newNotification = Notification.closedEventNotification(existingEvent, eventTranslation, recipientID, dbConnection.getUsername(recipientID), eventCost);
                                        dbConnection.insertNotification(newNotification);
                                    }
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                    System.exit(1);
                                }
                            }
                        }
                    }

                    break;
                }
                case NEW_EVENT: {
                    Event newEvent = null;
                    boolean canRegister = false;

                    try {
                        newEvent = it.unibs.ing.se.view.createEvent(currentUser, currentDateTime);
                    } catch (IllegalStateException e) {
                        System.err.println(menuTranslation.getTranslation("eventNotLegal"));
                        System.err.println(e.getMessage());
                        break;
                    }

                    if (newEvent == null) // User aborted event creation
                        break;

                    newEvent.updateState(currentDateTime); // UNKNOWN -> VALID
                    try {
                        dbConnection.insertEvent(newEvent);
                    } catch (SQLException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }

                    try {
                        canRegister = newEvent.register(currentUser);
                    } catch (IllegalArgumentException ex) {
                        if (ex.getMessage().contains("is already registered")) {
                            System.err.println(menuTranslation.getTranslation("userAlreadyRegisteredToEvent"));
                        } else if (ex.getMessage().contains("sex is not allowed")) {
                            System.err.println(menuTranslation.getTranslation("eventCreationSexMismatch"));
                        } else if (ex.getMessage().contains("age is not allowed")) {
                            System.err.println(menuTranslation.getTranslation("eventCreationAgeMismatch"));
                        }
                    } catch (IllegalStateException e) {
                        System.err.println(menuTranslation.getTranslation("eventRegistrationMaximumReached"));
                    }

                    if (canRegister) {
                        try {
                            dbConnection.insertOptionalCosts(it.unibs.ing.se.view.wantedOptionalCosts(newEvent.getOptionalCosts()), newEvent.getEventID(), currentUser.getUserID());
                            // If this kind of event has optional costs it prompts the user to choose which one he wants
                            // and saves them in the database
                            dbConnection.updateEventRegistrations(newEvent);
                        } catch (SQLException e) {
                            e.printStackTrace();
                            System.exit(1);
                        }
                    }

                    if (it.unibs.ing.se.view.publishEvent()) {
                        // The user wants to publish the event
                        try {
                            newEvent.publish(currentDateTime);
                            newEvent.updateState(currentDateTime); // VALID -> OPEN
                            try {
                                dbConnection.updateEventState(newEvent);
                                dbConnection.updateEventPublished(newEvent);
                            } catch (SQLException e) {
                                e.printStackTrace();
                                System.exit(1);
                            }
                        } catch (IllegalStateException e) {
                            System.err.println(e.getMessage());
                            break;
                        }

                        try { // Fire off notifications to all interested users
                            ArrayList<UUID> userIDs = dbConnection.getUserIDsByFavoriteCategory(newEvent.getEventType());
                            for (UUID userID : userIDs) {
                                Notification newEventNotification = Notification.newEventFavoriteCategoryNotification(newEvent, eventTranslation, userID, dbConnection.getUsername(userID));
                                dbConnection.insertNotification(newEventNotification);
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                            System.exit(1);
                        } catch (NoSuchElementException e) {
                            System.err.println(menuTranslation.getTranslation("nobodyInterestedInThisCategory"));
                        }

                        if (it.unibs.ing.se.view.sendInvite()) {
                            // The user wants to send invites
                            try { // Fire off notifications to all previously registered users
                                ArrayList<UUID> userIDs = dbConnection.getUserIDByOldRegistrations(currentUser.getUserID(), newEvent.getEventType());
                                for (UUID userID : userIDs) {
                                    Notification newEventNotification = Notification.newInviteNotification(newEvent, eventTranslation, userID, dbConnection.getUsername(userID), currentUser.getUsername());
                                    dbConnection.insertNotification(newEventNotification);
                                }
                            } catch (SQLException e) {
                                e.printStackTrace();
                                System.exit(1);
                            } catch (NoSuchElementException e) {
                                System.err.println(menuTranslation.getTranslation("nobodyRegisteredToYourEvents"));
                            }
                        }
                    }
                    break;
                }
                case HELP:
                    it.unibs.ing.se.view.displayHelp();
                    break;
                case LOGOUT:
                    currentUser = loginHelper.login();
                    break;
                case QUIT:
                    dbConnection.closeDb();
                    it.unibs.ing.se.view.printExit();
                    System.exit(0);
                default:
                    break;
            }

            currentDateTime = LocalDateTime.now(); // Refresh current date to allow event deadlines occurring now
            try {
                updateAllEvents(dbConnection, eventTranslation, currentDateTime);
            } catch (SQLException e) {
                e.printStackTrace();
                System.exit(1);
            }
            */

        }
    }

    /**
     * This method iterates over all the active events in the database and check if any needs to be updated.
     * If so it proceeds to update it and save it back into the database with its updated status.
     * If the new status require to send notifications to user it does so.
     * @param dbConnection Connector object with an already established connection to the database
     * @param eventTranslation it.unibs.ing.se.Main.jsonTranslator object with Event translation already opened
     * @param currentDateTime LocalDateTime object with the date to check against if status has to be updated or not
     * @throws SQLException If a database access error occurs
     */
    /* // TODO move this method somewhere else and change called methods
    static private void updateAllEvents(Connector dbConnection, JsonTranslator eventTranslation, LocalDateTime currentDateTime) throws SQLException {
        if (dbConnection == null) throw new IllegalStateException("ALERT: No connection to the database");

        ArrayList<Event> activeEvents;
        try {
            activeEvents = dbConnection.getActiveEvents();
        } catch (NoSuchElementException e) {
            // The database is empty
            return;
        }

        for (Event event : activeEvents) {
            if (event.updateState(currentDateTime)) {
                // event has to be updated since last execution of the software
                if (event.getCurrentState() == Event.State.CLOSED) {
                    // event is now in closed state, notifications have to be sent.
                    // If event was already closed we won't fall in this case
                    ArrayList<UUID> registeredUsers = event.getRegisteredUsers();
                    double eventCost;
                    if (! registeredUsers.contains(event.getCreatorID())) {
                        // Probably the creator could not join the even due to a sex mismatch, but it has to be informed as well
                        ArrayList<UUID> creatorCosts = dbConnection.getOptionalCosts(event.getCreatorID(), event.getEventID());
                        eventCost = event.totalCost(creatorCosts);
                        Notification newNotification = Notification.closedEventNotification(event, eventTranslation, event.getCreatorID(), dbConnection.getUsername(event.getCreatorID()), eventCost);
                        dbConnection.insertNotification(newNotification);
                    }
                    for (UUID recipientID : registeredUsers) {
                        ArrayList<UUID> userCosts = dbConnection.getOptionalCosts(recipientID, event.getEventID());
                        eventCost = event.totalCost(userCosts);
                        Notification newNotification = Notification.closedEventNotification(event, eventTranslation, recipientID, dbConnection.getUsername(recipientID), eventCost);
                        dbConnection.insertNotification(newNotification);
                    }
                } else if (event.getCurrentState() == Event.State.FAILED) {
                    ArrayList<UUID> registeredUsers = event.getRegisteredUsers();
                    if (! registeredUsers.contains(event.getCreatorID())) {
                        // Probably the creator could not join the even due to a sex mismatch, but it has to be informed as well
                        Notification newNotification = Notification.failedEventNotification(event, eventTranslation, event.getCreatorID(), dbConnection.getUsername(event.getCreatorID()));
                        dbConnection.insertNotification(newNotification);
                    }
                    for (UUID recipientID : registeredUsers) {
                        Notification newNotification = Notification.failedEventNotification(event, eventTranslation, recipientID, dbConnection.getUsername(recipientID));
                        dbConnection.insertNotification(newNotification);
                    }
                }
                dbConnection.updateEventState(event.getEventID(), event.getCurrentState());
            }
        }
    }
     */
}
