package it.unibs.ing.se.controller.helpers;

import it.unibs.ing.se.DMO.Connector;
import it.unibs.ing.se.DMO.JsonTranslator;
import it.unibs.ing.se.model.Event;
import it.unibs.ing.se.model.Notification;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.UUID;

public class NotificationHelper {
    private Connector dbConnection;
    private JsonTranslator menuTranslation;
    private JsonTranslator eventTranslation;
    private Event event;

    NotificationHelper(UUID eventID) {
        this.menuTranslation = new JsonTranslator(JsonTranslator.MENU_JSON_PATH);
        this.eventTranslation = new JsonTranslator(JsonTranslator.EVENT_JSON_PATH);
        dbConnection = Connector.getInstance();
        try {
            event = dbConnection.getEvent(eventID);
        } catch (SQLException e) {
            System.err.println(menuTranslation.getTranslation("SQLError"));
            System.exit(1);
        }
    }

    /**
     * This methods sends notifications basing only on event's current state.
     * If called multiple times WILL send multiple duplicate notifications, so it has
     * to be called only when appropriate (e.g. when event.updateState returns true)
     */
    void send() {
        ArrayList<UUID> registeredUsers;
        switch (event.getCurrentState()) {
            case OPEN:
                try { // Fire off notifications to all interested users
                    ArrayList<UUID> userIDs = dbConnection.getUserIDsByFavoriteCategory(event.getEventType());
                    for (UUID userID : userIDs) {
                        Notification newEventNotification = newEventFavoriteCategoryNotification(userID, dbConnection.getUsername(userID));
                        dbConnection.insertNotification(newEventNotification);
                    }
                } catch (SQLException e) {
                    System.err.println(menuTranslation.getTranslation("SQLError"));
                    System.exit(1);
                } catch (NoSuchElementException e) {
                    System.err.println(menuTranslation.getTranslation("nobodyInterestedInThisCategory"));
                }
                break;
            case WITHDRAWN:
                registeredUsers = event.getRegisteredUsers();
                for (UUID recipientID : registeredUsers) {
                    try {
                        Notification newNotification = withdrawnEventNotification(recipientID, dbConnection.getUsername(recipientID));
                        dbConnection.insertNotification(newNotification);
                    } catch (SQLException e) {
                        System.err.println(menuTranslation.getTranslation("SQLError"));
                        System.exit(1);
                    }
                }
                break;
            case CLOSED:
                String recipientUsername;
                try {
                    registeredUsers = event.getRegisteredUsers();
                    double eventCost;
                    if (! registeredUsers.contains(event.getCreatorID())) {
                        String creatorUsername = dbConnection.getUsername(event.getCreatorID());
                        ArrayList<UUID> creatorCosts = dbConnection.getOptionalCosts(event.getCreatorID(), event.getEventID());
                        eventCost = event.totalCost(creatorCosts);
                        // Probably the creator could not join the even due to a sex mismatch, but it has to be informed as well
                        Notification newNotification = closedEventNotification(event.getCreatorID(), creatorUsername, eventCost);
                        dbConnection.insertNotification(newNotification);
                    }
                    for (UUID recipientID : registeredUsers) {
                        recipientUsername = dbConnection.getUsername(event.getCreatorID());
                        ArrayList<UUID> userCosts = dbConnection.getOptionalCosts(recipientID, event.getEventID());
                        eventCost = event.totalCost(userCosts);
                        Notification newNotification = closedEventNotification(recipientID, recipientUsername, eventCost);
                        dbConnection.insertNotification(newNotification);
                    }
                } catch (SQLException e) {
                    System.err.println(menuTranslation.getTranslation("SQLError"));
                    System.exit(1);
                }
                break;
            case FAILED:
                registeredUsers = event.getRegisteredUsers();
                try {
                    if (! registeredUsers.contains(event.getCreatorID())) {
                        // Probably the creator could not join the even due to a sex mismatch, but it has to be informed as well
                        Notification newNotification = failedEventNotification(event.getCreatorID(), dbConnection.getUsername(event.getCreatorID()));
                        dbConnection.insertNotification(newNotification);
                    }
                    for (UUID recipientID : registeredUsers) {
                        Notification newNotification = failedEventNotification(recipientID, dbConnection.getUsername(recipientID));
                        dbConnection.insertNotification(newNotification);
                    }
                } catch (SQLException e) {
                    System.err.println(menuTranslation.getTranslation("SQLError"));
                    System.exit(1);
                }
                break;
        }
    }

    void sendInvites() {
        try { // Fire off notifications to all previously registered users
            ArrayList<UUID> userIDs = dbConnection.getUserIDByOldRegistrations(event.getCreatorID(), event.getEventType());
            String creatorUsername;
            for (UUID userID : userIDs) {
                creatorUsername = dbConnection.getUsername(event.getCreatorID());
                Notification newEventNotification = newInviteNotification(userID, dbConnection.getUsername(userID), creatorUsername);
                dbConnection.insertNotification(newEventNotification);
            }
        } catch (SQLException e) {
            System.err.println(menuTranslation.getTranslation("SQLError"));
            System.exit(1);
        } catch (NoSuchElementException e) {
            System.err.println(menuTranslation.getTranslation("nobodyRegisteredToYourEvents"));
        }
    }

    /**
     * Creates a Notification object with strings related to an event being CLOSED with the needed number of participants
     * @param recipientID UUID of the user to send the notification to
     * @param recipientUsername String with username of the user to send the notification to
     * @return Notification object with all the values instantiated
     */
    public Notification closedEventNotification(UUID recipientID, String recipientUsername, double eventCost) {
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(" yyyy-MM-dd HH:mm ");

        UUID notificationID = UUID.randomUUID();
        UUID eventID = event.getEventID();
        boolean read = false;
        String title = String.format(eventTranslation.getTranslation("eventSuccessTitle"), event.title);

        sb.append(String.format(eventTranslation.getTranslation("eventSuccessContentIntro"), recipientUsername)).append('\n');
        sb.append(String.format(eventTranslation.getTranslation("eventSuccessContentStartDate"), event.startDate.format(dateFormatter)));
        if (event.endDate != null)
            sb.append(String.format(eventTranslation.getTranslation("eventSuccessContentEndDate"), event.endDate.format(dateFormatter)));
        if (event.endDate != null)
            sb.append(String.format(eventTranslation.getTranslation("eventSuccessContentDuration"), event.duration).replace("PT", ""));
        sb.append('\n').append(String.format(eventTranslation.getTranslation("eventSuccessContentCost"), eventCost)).append('\n');
        sb.append(String.format(eventTranslation.getTranslation("eventSuccessContentConclusion"), event.location));
        String content = sb.toString();

        return new Notification(notificationID, eventID, recipientID, read, title, content);
    }

    /**
     * Creates a Notification object with strings related to an event being FAILED
     * @param recipientID UUID of the user to send the notification to
     * @param recipientUsername String with username of the user to send the notification to
     * @return Notification object with all the values instantiated
     */
    public Notification failedEventNotification(UUID recipientID, String recipientUsername) {
        UUID notificationID = UUID.randomUUID();
        UUID eventID = event.getEventID();
        boolean read = false;
        String title = String.format(eventTranslation.getTranslation("eventFailTitle"), event.title);
        String content = String.format(eventTranslation.getTranslation("eventFailContent"), recipientUsername);

        return new Notification(notificationID, eventID, recipientID, read, title, content);
    }

    /**
     * Creates a Notification object with strings related to an event being WITHDRAWN due to creator's deregistration
     * @param recipientID UUID of the user to send the notification to
     * @param recipientUsername String with username of the user to send the notification to
     * @return Notification object with all the values instantiated
     */
    public Notification withdrawnEventNotification(UUID recipientID, String recipientUsername) {
        UUID notificationID = UUID.randomUUID();
        UUID eventID = event.getEventID();
        boolean read = false;
        String title = String.format(eventTranslation.getTranslation("eventWithdrawnTitle"), event.title);
        String content = String.format(eventTranslation.getTranslation("eventWithdrawnContent"), recipientUsername);

        return new Notification(notificationID, eventID, recipientID, read, title, content);
    }

    /**
     * Creates a Notification object with strings related to a new event of a favorite category being published
     * @param recipientID UUID of the user to send the notification to
     * @param recipientUsername String with username of the user to send the notification to
     * @return Notification object with all the values instantiated
     */
    public Notification newEventFavoriteCategoryNotification(UUID recipientID, String recipientUsername) {
        UUID notificationID = UUID.randomUUID();
        UUID eventID = event.getEventID();
        boolean read = false;
        String title = eventTranslation.getTranslation("eventFavoriteCategoryTitle");
        String content = String.format(eventTranslation.getTranslation("eventFavoriteCategoryContent"), recipientUsername, event.title);

        return new Notification(notificationID, eventID, recipientID, read, title, content);
    }

    /**
     * Creates a Notification object with strings related to a new event of a favorite category being published
     * @param recipientID UUID of the user to send the notification to
     * @param recipientUsername String with username of the user to send the notification to
     * @param senderUsername String with username of the user which has created the event
     * @return Notification object with all the values instantiated
     */
    public Notification newInviteNotification(UUID recipientID, String recipientUsername, String senderUsername) {
        UUID notificationID = UUID.randomUUID();
        UUID eventID = event.getEventID();
        boolean read = false;
        String title = eventTranslation.getTranslation("eventInviteTitle");
        String content = String.format(eventTranslation.getTranslation("eventInviteContent"), recipientUsername, senderUsername, event.title);

        return new Notification(notificationID, eventID, recipientID, read, title, content);
    }
}
