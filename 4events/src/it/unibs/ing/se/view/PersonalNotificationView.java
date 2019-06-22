package it.unibs.ing.se.view;

import it.unibs.ing.se.DMO.Connector;
import it.unibs.ing.se.DMO.JsonTranslator;
import it.unibs.ing.se.model.Notification;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;

public class PersonalNotificationView implements PrintableInterface<ArrayList<UUID>> {
    private JsonTranslator translation;
    private Connector dbConnection;
    private ArrayList<UUID> notificationsIDs;

    public PersonalNotificationView(UUID currentUserID) {
        this.translation = JsonTranslator.getInstance();
        dbConnection = Connector.getInstance();
        try {
            notificationsIDs = dbConnection.getAllNotifications(currentUserID);
        } catch (SQLException e) {
            System.err.println(translation.getTranslation("SQLError"));
            System.exit(1);
        }
    }

    @Override
    public void print() {
        if (notificationsIDs == null) {
            System.out.println(translation.getTranslation("noPersonalNotifications"));
            return;
        }

        StringBuilder sb = new StringBuilder(); // Lots of eye candy from here on
        sb.append(translation.getTranslation("welcomeNotification")).append('\n');

        String notificationRead = translation.getTranslation("notificationRead");
        String notificationUnread = translation.getTranslation("notificationUnread");
        int maxLen;
        if (notificationRead.length() > notificationUnread.length()) // Avoids fixed length to allow future translations
            maxLen = notificationRead.length();
        else
            maxLen = notificationUnread.length();

        String notificationReadSpacer = new String(new char[maxLen - notificationRead.length()]).replace('\0', ' ');
        String notificationUnreadSpacer = new String(new char[maxLen - notificationUnread.length()]).replace('\0', ' ');

        for (int i = 0; i < notificationsIDs.size(); i++) {
            Notification notification = null;
            try {
                notification = dbConnection.getNotification(notificationsIDs.get(i));
            } catch (SQLException e) {
                System.err.println(translation.getTranslation("SQLError"));
                System.exit(1);
            }
            String notificationStatus = notification.isRead() ? notificationRead : notificationUnread;
            String statusSpacer = notification.isRead() ? notificationReadSpacer : notificationUnreadSpacer;

            sb.append(i + 1).append(") ");
            sb.append(notificationStatus);
            sb.append(statusSpacer);
            sb.append(" - ");
            sb.append(notification.getTitle()).append('\n');
            sb.append(notification.getContent()).append('\n');
        }
        System.out.println(sb);
    }

    @Override
    public ArrayList<UUID> parseInput() {
        if (notificationsIDs == null) {
            return null;
        }

        ArrayList<Integer> userNumbers = InputManager.inputNumberSequence(translation.getTranslation("selectNotificationToSetAsRead"), true);
        ArrayList<UUID> selectedNotifications = new ArrayList<>();
        if (userNumbers == null) {
            System.out.println(translation.getTranslation("invalidUserSelection"));
            return null;
        }
        for (Integer number : userNumbers)
            if (number - 1 >= notificationsIDs.size() || number - 1 < 0) { // Notifications are printed with 1 based index
                System.err.println(translation.getTranslation("invalidUserSelection"));
                return null; // out of bound
            } else {
                selectedNotifications.add(notificationsIDs.get(number - 1));
            }

        return selectedNotifications;
    }
}
