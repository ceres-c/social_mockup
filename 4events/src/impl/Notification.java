package impl;


import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class Notification {
    private UUID notificationID;
    private UUID eventID;
    private UUID recipientID;
    private boolean read;
    private String title;
    private String content;

    public Notification(UUID notificationID, UUID eventID, UUID recipientID, boolean read, String title, String content) {
        this.notificationID = notificationID;
        this.eventID = eventID;
        this.recipientID = recipientID;
        this.read = read;
        this.title = title;
        this.content = content;
    }

    public UUID getNotificationID() {
        return notificationID;
    }

    public UUID getEventID() { return eventID; }

    public UUID getRecipientID() {
        return recipientID;
    }

    public boolean isRead() {
        return read;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public void setNotificationID(UUID notificationID) {
        this.notificationID = notificationID;
    }

    public void setEventID(UUID eventID) {
        this.eventID = eventID;
    }

    public void setRecipientID(UUID recipientID) {
        this.recipientID = recipientID;
    }

    public void setNotificationID(String notificationID) {
        this.notificationID = UUID.fromString(notificationID);
    }

    public void setEventID(String eventID) {
        this.eventID = UUID.fromString(eventID);
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public void setRecipientID(String recipientID) {
        this.recipientID = UUID.fromString(recipientID);
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Creates a Notification object with strings related to an event being CLOSED with the needed number of participants
     * @param event Event object that is closed
     * @param eventTranslation Main.jsonTranslator object with Event translation already opened
     * @param recipientID UUID of the user to send the notification to
     * @param username String with username of the user to send the notification to
     * @return Notification object with all the values instantiated
     */
    static Notification closedEventNotification(Event event, Main.jsonTranslator eventTranslation, UUID recipientID, String username) {
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(" yyyy-MM-dd HH:mm ");

        UUID notificationID = UUID.randomUUID();
        UUID eventID = event.getEventID();
        boolean read = false;
        String title = String.format(eventTranslation.getTranslation("eventSuccessTitle"), event.title);

        sb.append(String.format(eventTranslation.getTranslation("eventSuccessContentIntro"), username)).append('\n');
        sb.append(String.format(eventTranslation.getTranslation("eventSuccessContentStartDate"), event.startDate.format(dateFormatter)));
        if (event.endDate != null)
            sb.append(String.format(eventTranslation.getTranslation("eventSuccessContentEndDate"), event.endDate.format(dateFormatter)));
        if (event.endDate != null)
            sb.append(String.format(eventTranslation.getTranslation("eventSuccessContentDuration"), event.duration).replace("PT", ""));
        sb.append('\n').append(String.format(eventTranslation.getTranslation("eventSuccessContentCost"), event.cost)).append('\n');
        sb.append(String.format(eventTranslation.getTranslation("eventSuccessContentConclusion"), event.location));
        String content = sb.toString();

        return new Notification(notificationID, eventID, recipientID, read, title, content);
    }

    /**
     * Creates a Notification object with strings related to an event being FAILED
     * @param event Event object that is closed
     * @param eventTranslation Main.jsonTranslator object with Event translation already opened
     * @param recipientID UUID of the user to send the notification to
     * @param username String with username of the user to send the notification to
     * @return Notification object with all the values instantiated
     */
    static Notification failedEventNotification(Event event, Main.jsonTranslator eventTranslation, UUID recipientID, String username) {
        UUID notificationID = UUID.randomUUID();
        UUID eventID = event.getEventID();
        boolean read = false;
        String title = String.format(eventTranslation.getTranslation("eventFailTitle"), event.title);
        String content = String.format(eventTranslation.getTranslation("eventFailContent"), username);

        return new Notification(notificationID, eventID, recipientID, read, title, content);
    }

    /**
     * Creates a Notification object with strings related to an event being WITHDRAWN due to creator's deregistration
     * @param event Event object that is closed
     * @param eventTranslation Main.jsonTranslator object with Event translation already opened
     * @param recipientID UUID of the user to send the notification to
     * @param username String with username of the user to send the notification to
     * @return Notification object with all the values instantiated
     */
    static Notification withdrawnEventNotification(Event event, Main.jsonTranslator eventTranslation, UUID recipientID, String username) {
        UUID notificationID = UUID.randomUUID();
        UUID eventID = event.getEventID();
        boolean read = false;
        String title = String.format(eventTranslation.getTranslation("eventWithdrawnTitle"), event.title);
        String content = String.format(eventTranslation.getTranslation("eventWithdrawnContent"), username);

        return new Notification(notificationID, eventID, recipientID, read, title, content);
    }
}