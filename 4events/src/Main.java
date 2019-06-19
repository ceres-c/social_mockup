import DMO.Connector;
import DMO.JsonConfigReader;
import DMO.JsonTranslator;
import menu.Menu;
import menu.commands.Command;
import menu.commands.DashboardCommand;
import model.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

public class Main {

    private static final String CONFIG_JSON_PATH = "config.json";

    public static void main(String[] args) {
        Connector myConnector = null; // Declared null to shut the compiler up, on usage it will always be properly instanced

        Path configJsonPath = Paths.get(CONFIG_JSON_PATH);
        JsonConfigReader config = new JsonConfigReader(configJsonPath.toString());

        Path menuJsonPath = Paths.get(Menu.MENU_JSON_PATH);
        JsonTranslator menuTranslation = new JsonTranslator(menuJsonPath.toString());

        Path eventJsonPath = Paths.get(Event.getJsonPath());
        JsonTranslator eventTranslation = new JsonTranslator(eventJsonPath.toString());

        LocalDateTime currentDateTime = LocalDateTime.now();
        User currentUser;

        try {
            myConnector = new Connector(config.getDBURL(), config.getDBUser(), config.getDBPassword());
        } catch (SQLException e) {
            System.out.println("ALERT: Error establishing a database connection!");
            e.printStackTrace();
            System.exit(1);
        }

        try {
            updateAllEvents(myConnector, eventTranslation, currentDateTime);
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }

        Menu menu = Menu.getInstance(myConnector, menuTranslation);
        menu.printWelcome();

        while ((currentUser = menu.loginOrSignup()) == null);

        Command userSelection = Command.INVALID;

        while (true) {
            userSelection = menu.displayMainMenu(currentUser);
            switch (userSelection) {
                case INVALID:
                    break;
                case DASHBOARD:
                    DashboardCommand dashboardUserSelection = menu.displayDashboard();
                    switch (dashboardUserSelection) {
                        case USER_PROFILE:
                            User newUser = menu.displayAndEditUserProfile(currentUser);
                            if (!newUser.equals(currentUser)) { // User made some changes to its profile
                                try {
                                    myConnector.updateUser(newUser); // Then Update the database
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                    System.exit(1);
                                }
                                currentUser = newUser; // And refresh currentUser
                            }
                            break;
                        case PERSONAL_NOTIFICATIONS:
                            menu.displayNotifications(currentUser);
                            break;
                        case CREATED_EVENTS:
                            Event existingEvent = menu.displayAndSelectCreatedEvents(currentUser);
                            if (existingEvent != null) {
                                // User want to publish an event which wasn't published before
                                System.out.println(existingEvent.detailedDescription(eventTranslation, myConnector));

                                if (menu.publishEvent()) {
                                    // The user wants to publish the event
                                    try {
                                        existingEvent.publish(currentDateTime);
                                        existingEvent.updateState(currentDateTime); // VALID -> OPEN
                                        try {
                                            myConnector.updateEventState(existingEvent);
                                            myConnector.updateEventPublished(existingEvent);
                                        } catch (SQLException e) {
                                            e.printStackTrace();
                                            System.exit(1);
                                        }
                                    } catch (IllegalStateException e) {
                                        System.err.println(e.getMessage());
                                        break;
                                    }
                                } else if (menu.withdrawEvent()) {
                                    // The user wants to withdraw the event
                                    existingEvent.setEventWithdrawn();
                                    try {
                                        myConnector.updateEventState(existingEvent);
                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                        System.exit(1);
                                    }

                                    // Generate notifications
                                    ArrayList<UUID> registeredUsers = existingEvent.getRegisteredUsers();
                                    for (UUID recipientID : registeredUsers) {
                                        try {
                                            Notification newNotification = Notification.withdrawnEventNotification(existingEvent, eventTranslation, recipientID, myConnector.getUsername(recipientID));
                                            myConnector.insertNotification(newNotification);
                                        } catch (SQLException e) {
                                            e.printStackTrace();
                                            System.exit(1);
                                        }
                                    }
                                }
                            }
                            break;
                        case REGISTERED_EVENTS:
                            menu.displayEventsByRegistration(currentUser, currentDateTime);
                            break;
                        default:
                            System.out.println(menuTranslation.getTranslation("invalidUserSelection"));
                            break;
                    }
                    break;
                case PUBLIC_EVENTS_LIST: {
                    Event existingEvent;
                    existingEvent = menu.chooseEventFromPublicList();

                    if (existingEvent != null) {
                        // Means the user has selected an event from the list
                        System.out.println(existingEvent.detailedDescription(eventTranslation, myConnector));

                        if (menu.registerEvent()) {
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
                                    myConnector.insertOptionalCosts(menu.wantedOptionalCosts(existingEvent.getOptionalCosts()), existingEvent.getEventID(), currentUser.getUserID());
                                    // If this kind of event has optional costs it prompts the user to choose which one he wants
                                    // and saves them in the database
                                    myConnector.updateEventRegistrations(existingEvent);
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                    System.exit(1);
                                }
                            }

                            if (existingEvent.updateState(currentDateTime)) {
                                // This registration has brought the number of registered users to the wanted participants number
                                try {
                                    myConnector.updateEventState(existingEvent);
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                    System.exit(1);
                                }
                                // We have to fire off notifications
                                try {
                                    ArrayList<UUID> registeredUsers = existingEvent.getRegisteredUsers();
                                    double eventCost;
                                    if (! registeredUsers.contains(existingEvent.getCreatorID())) {
                                        ArrayList<UUID> creatorCosts = myConnector.getOptionalCosts(existingEvent.getCreatorID(), existingEvent.getEventID());
                                        eventCost = existingEvent.totalCost(creatorCosts);
                                        // Probably the creator could not join the even due to a sex mismatch, but it has to be informed as well
                                        Notification newNotification = Notification.closedEventNotification(existingEvent, eventTranslation, existingEvent.getCreatorID(), myConnector.getUsername(existingEvent.getCreatorID()), eventCost);
                                        myConnector.insertNotification(newNotification);
                                    }
                                    for (UUID recipientID : registeredUsers) {
                                        ArrayList<UUID> userCosts = myConnector.getOptionalCosts(recipientID, existingEvent.getEventID());
                                        eventCost = existingEvent.totalCost(userCosts);
                                        Notification newNotification = Notification.closedEventNotification(existingEvent, eventTranslation, recipientID, myConnector.getUsername(recipientID), eventCost);
                                        myConnector.insertNotification(newNotification);
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
                        newEvent = menu.createEvent(currentUser, currentDateTime);
                    } catch (IllegalStateException e) {
                        System.err.println(menuTranslation.getTranslation("eventNotLegal"));
                        System.err.println(e.getMessage());
                        break;
                    }

                    if (newEvent == null) // User aborted event creation
                        break;

                    newEvent.updateState(currentDateTime); // UNKNOWN -> VALID
                    try {
                        myConnector.insertEvent(newEvent);
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
                            myConnector.insertOptionalCosts(menu.wantedOptionalCosts(newEvent.getOptionalCosts()), newEvent.getEventID(), currentUser.getUserID());
                            // If this kind of event has optional costs it prompts the user to choose which one he wants
                            // and saves them in the database
                            myConnector.updateEventRegistrations(newEvent);
                        } catch (SQLException e) {
                            e.printStackTrace();
                            System.exit(1);
                        }
                    }

                    if (menu.publishEvent()) {
                        // The user wants to publish the event
                        try {
                            newEvent.publish(currentDateTime);
                            newEvent.updateState(currentDateTime); // VALID -> OPEN
                            try {
                                myConnector.updateEventState(newEvent);
                                myConnector.updateEventPublished(newEvent);
                            } catch (SQLException e) {
                                e.printStackTrace();
                                System.exit(1);
                            }
                        } catch (IllegalStateException e) {
                            System.err.println(e.getMessage());
                            break;
                        }

                        try { // Fire off notifications to all interested users
                            ArrayList<UUID> userIDs = myConnector.getUserIDsByFavoriteCategory(newEvent.getEventType());
                            for (UUID userID : userIDs) {
                                Notification newEventNotification = Notification.newEventFavoriteCategoryNotification(newEvent, eventTranslation, userID, myConnector.getUsername(userID));
                                myConnector.insertNotification(newEventNotification);
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                            System.exit(1);
                        } catch (NoSuchElementException e) {
                            System.err.println(menuTranslation.getTranslation("nobodyInterestedInThisCategory"));
                        }

                        if (menu.sendInvite()) {
                            // The user wants to send invites
                            try { // Fire off notifications to all previously registered users
                                ArrayList<UUID> userIDs = myConnector.getUserIDByOldRegistrations(currentUser.getUserID(), newEvent.getEventType());
                                for (UUID userID : userIDs) {
                                    Notification newEventNotification = Notification.newInviteNotification(newEvent, eventTranslation, userID, myConnector.getUsername(userID), currentUser.getUsername());
                                    myConnector.insertNotification(newEventNotification);
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
                    menu.displayHelp();
                    break;
                case LOGOUT:
                    while ((currentUser = menu.loginOrSignup()) == null); // Simply refreshes currentUser object
                    break;
                case QUIT:
                    myConnector.closeDb();
                    menu.printExit();
                    System.exit(0);
                default:
                    break;
            }

            currentDateTime = LocalDateTime.now(); // Refresh current date to allow event deadlines occurring now
            try {
                updateAllEvents(myConnector, eventTranslation, currentDateTime);
            } catch (SQLException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    /**
     * This method iterates over all the active events in the database and check if any needs to be updated.
     * If so it proceeds to update it and save it back into the database with its updated status.
     * If the new status require to send notifications to user it does so.
     * @param dbConnection Connector object with an already established connection to the database
     * @param eventTranslation Main.jsonTranslator object with Event translation already opened
     * @param currentDateTime LocalDateTime object with the date to check against if status has to be updated or not
     * @throws SQLException If a database access error occurs
     */
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
                dbConnection.updateEventState(event);
            }
        }
    }

}
