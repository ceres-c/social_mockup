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
}
