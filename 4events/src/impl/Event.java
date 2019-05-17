package impl;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.UUID;

import interfaces.LegalObject;
import interfaces.ReflectionInterface;

/**
 * This abstract class represents generic events that will be inflected
 * into specific categories via extending classes
 */
abstract class Event implements LegalObject, ReflectionInterface {
    public enum State {
        UNKNOWN,
        VALID,
        OPEN,
        CLOSED,
        FAILED,
        ENDED
    }

    private static final String EVENT_JSON_PATH = "res/IT_EventDescr.json";

    private UUID            eventID; // UUID of the event stored in the DB
    private UUID            creatorID; // UUID of the user who created the event
    private String          eventTypeDB; // As present in DB category list: ie. "soccer_game"
    private boolean         published;
    ArrayList<UUID>         registeredUsers;
    private State           currentState;
    public  String          title;
    public  Integer         participantsNum;
    public  LocalDateTime   deadline;
    public  String          location;
    public  LocalDateTime   startDate;
    public  Duration        duration;
    public  Double          cost;
    public  String          inQuota;
    public  LocalDateTime   endDate;
    public  String          notes;

    private final String[] mandatoryFields = {"participantsNum", "deadline", "location", "startDate", "cost"};

    Event(UUID eventID, UUID creatorID, String catDb) {
        this.eventID = eventID;
        this.creatorID = creatorID;
        this.eventTypeDB = catDb;
        this.published = false;
        this.currentState = State.UNKNOWN;
        this.registeredUsers = new ArrayList<>();
    }

    public UUID getEventID() { return eventID; }

    public String getEventIDAsString() { return eventID.toString(); }

    public String getCreatorID() { return creatorID.toString(); }

    public String getEventTypeDB() { return eventTypeDB; }

    public boolean isPublished () { return published; }

    /**
     *
     * @return Returns an ArrayList with String representation of registered users' UUID
     */
    public ArrayList<String> getRegisteredUsersAsString() {
        ArrayList<String> registeredUsersString = new ArrayList<>();
        for(UUID registeredUser : registeredUsers){
            registeredUsersString.add(registeredUser.toString());
        }
        return registeredUsersString;
    }

    public String getState () { return this.currentState.name(); }

    static String getJsonPath() {
        return EVENT_JSON_PATH;
    }

    /**
     * A method to set an object as published.
     * updateStatus should be called afterwards to check for differences and update the database accordingly
     * @return true if this Event can be and has been published
     * @throws IllegalStateException If called on an event which is non-legal
     */
    boolean publish() throws IllegalStateException {
        if (!isLegal())
            throw new IllegalStateException("ALERT: Non legal events can't be published");
        published = true;
        return true;
    }

    /**
     * Setter that has to be used ONLY to restore an event from the database. For normal operation use publish
     * @param state
     */
    public void setPublished (boolean state) { this.published = state; }

    /**
     * A method to save an UserID into the Event object to keep track of registered users
     * This overloaded method has to be used when a user creates an event or register for it
     * @param user A User object from whose the UserID will be taken
     * @return True if everything went smoothly
     * @throws IllegalArgumentException If anything goes wrong while registering the user (error in Exception message)
     *                                  i.e User is already registered or User's Sex is not appropriate for this event
     */
    abstract boolean register(User user) throws IllegalArgumentException;

    /**
     * A method to save an UserID into the Event object to keep track of registered users
     * This overloaded method has to be used to restore an event from the database
     * @param userID A UUID object
     * @return True if everything went smoothly
     * @throws IllegalArgumentException If anything goes wrong while registering the user (error in Exception message)
     *                                  i.e User is already registered
     */
    abstract boolean register(UUID userID) throws IllegalArgumentException;

    /**
     * Setter that has to be used ONLY to restore an event from the database. For normal operation use updateStatus.
     * @param state
     */
    public void setCurrentState (String state) { currentState = State.valueOf(state); }

    /**
     * Control the "Status" state machine basing on current Status and date/time.
     * @param currentDateTime
     * @return True if Event's status has changed and then the event has to be updated in the database as well
     */
    public boolean updateStatus (LocalDateTime currentDateTime) {
        switch (currentState) {
            case UNKNOWN:
                if (this.isLegal()) {
                    currentState = State.VALID;
                    return true;
                }
                break;

            case VALID:
                if (this.published) {
                    currentState = State.OPEN;
                    return true;
                }
                break;

            case OPEN:
                if (currentDateTime.isBefore(deadline) || currentDateTime.equals(deadline)) {
                    if (registeredUsers.size() >= participantsNum) {
                        this.currentState = State.CLOSED;
                        return true;
                    }
                } else {
                    if (registeredUsers.size() < participantsNum) {
                        this.currentState = State.FAILED;
                    } else if (registeredUsers.size() >= participantsNum) {
                        this.currentState = State.CLOSED;
                    }
                    return true;
                }
                break;

            case CLOSED:
                if (currentDateTime.isAfter(endDate)) {
                    this.currentState = State.ENDED;
                    return true;
                }
                break;

            default:
                return false; // We're in ENDED or FAILED statuses and those can't be altered
        }
        return false;
    }

    /**
     * A method to get the fields of a class and its fathers
     * @return a LinkedHashMap with a String as a key and a Class<?> as Value
     *   - Key is field's name as a String (such as the one returned from getAttributesName)
     *   - Value is the internal type of the field
     */
    public LinkedHashMap<String, Class<?>> getAttributesWithType(){
        Field[] superFields = this.getClass().getSuperclass().getFields(); // Only public fields
        Field[] thisFields = this.getClass().getDeclaredFields(); // Both public and private fields

        LinkedHashMap<String, Class<?>> returnFields = new LinkedHashMap<>();

        for (Field field:superFields) {
            /*
             * Following line return only the name of the field instead of full class name + field.
             * It gets the last occurrence of the '.' char, add 1 to it (to exclude the dot itself)
             * and then trims the string gotten from the reflection.
             */
            String fieldName = field.toString().substring( field.toString().lastIndexOf('.') + 1 );
            returnFields.put(fieldName, field.getType());
        }
        for (Field field:thisFields) {
            if (field.getModifiers() == Modifier.PUBLIC) { // Filter out only public fields
                // Same goes for this line
                String fieldName = field.toString().substring( field.toString().lastIndexOf('.') + 1 );
                returnFields.put(fieldName, field.getType());
            }
        }
        return returnFields;
    }

    /**
     * A method to get DB-input ready values of all non-null fields of a class and its fathers
     * @return a LinkedHashMap with a String as a key and an Object as Value
     *   - Key is field's name as a String (such as the one returned from getAttributesName)
     *   - Value is an Object with current value of a field
     */
    public LinkedHashMap<String, Object> getNonNullAttributesWithValue() {
        Field[] superFields = this.getClass().getSuperclass().getFields(); // Only public fields
        Field[] thisFields = this.getClass().getDeclaredFields(); // Both public and private fields

        LinkedHashMap<String, Object> returnFields = new LinkedHashMap<>();

        for (Field field:superFields) {
            /*
             * Following line return only the name of the field instead of full class name + field.
             * It gets the last occurrence of the '.' char, add 1 to it (to exclude the dot itself)
             * and then trims the string gotten from the reflection.
             */
            try {
                if (field.get(this) != null) {
                    String fieldName = field.toString().substring(field.toString().lastIndexOf('.') + 1);
                    returnFields.put(fieldName, field.get(this));
                }
            } catch (IllegalAccessException e) {
                System.out.println("ALERT: Illegal access on field: " + field);
                e.printStackTrace();
            }
        }
        for (Field field:thisFields) {
            try {
                if (field.getModifiers() == Modifier.PUBLIC && field.get(this) != null) { // Filter out only public fields
                    // Same goes for this line
                    String fieldName = field.toString().substring( field.toString().lastIndexOf('.') + 1 );
                    returnFields.put(fieldName, field.get(this));
                }
            } catch (IllegalAccessException e) {
                System.out.println("ALERT: Illegal access on field: " + field);
                e.printStackTrace();
            }
        }
        return returnFields;
    }

    /**
     * A method to get the names of public fields of a class and its fathers
     * @return an ArrayList of Strings
     */
    public ArrayList<String> getAttributesName(){

        Field[] superFields = this.getClass().getSuperclass().getFields(); // Only public fields
        Field[] thisFields = this.getClass().getDeclaredFields(); // Both public and private fields

        ArrayList <String> returnFields = new ArrayList<>();

        for (Field field:superFields) {
            /*
             * Following line return only the name of the field instead of full class name + field.
             * It gets the last occurrence of the '.' char, add 1 to it (to exclude the dot itself)
             * and then trims the string gotten from the reflection.
             */
            String fieldName = field.toString().substring( field.toString().lastIndexOf('.') + 1 );
            returnFields.add(fieldName);
        }
        for (Field field:thisFields) {
            if (field.getModifiers() == Modifier.PUBLIC) { // Filter out only public fields
                // Same goes for this line
                String fieldName = field.toString().substring( field.toString().lastIndexOf('.') + 1 );
                returnFields.add(fieldName);
            }
        }
        return returnFields;
    }

    /**
     * A method to set a field to a given object passed from the caller
     */
    public void setAttribute(String fieldName, Object content) {
        try {
            Field field = this.getClass().getSuperclass().getDeclaredField( fieldName ); // Need to get the super class as this is an ancestor of real instance classes
            field.set(this, content);
        } catch (NoSuchFieldException e) {
            System.out.println("ALERT: Missing field: " + fieldName);
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            System.out.println("ALERT: Illegal access on field: " + fieldName);
            e.printStackTrace();
        }
    }

    /**
     * A method to check if a field is mandatory or optional
     */
    public boolean isOptional(String fieldName) {
        for (String field:mandatoryFields)
            if (fieldName.equals(field)) return false;
        return true;
    }

    /**
     * A method to check if the values input by an user are logically valid, used before saving to the DB.
     * Subclasses should override and call this method with relevant checks.
     * @return boolean:
     *      - True if legal
     * @throws IllegalStateException if user input isn't legal
     */
    public boolean isLegal() throws IllegalStateException {
        LocalDateTime currentDate = LocalDateTime.now();

        if (startDate.isBefore(deadline))
            throw new IllegalStateException ("ALERT: start date prior than specified deadline");
        if (currentDate.isAfter(deadline))
            throw new IllegalStateException ("ALERT: deadline in the past");
        if (endDate != null && endDate.isBefore(startDate) && !endDate.equals(startDate))
            throw new IllegalStateException ("ALERT: end date prior start date");
        if (duration != null) {
            if (endDate != null && endDate.isBefore(startDate.plus(duration)))
                throw new IllegalStateException("ALERT: end date comes before start date + duration");
        }

        return true;
    }

    @Override
    public String toString () {
        StringBuilder sb = new StringBuilder();
        sb.append("Title: ").append(title).append("\n");
        sb.append("Creator ID: ").append(creatorID.toString()).append("\n");
        sb.append("Database Event Type: ").append(eventTypeDB).append("\n");
        sb.append("Published: ").append(published).append("\n");
        sb.append("Status: ").append(currentState.name()).append("\n");
        sb.append("Registered users: \n");
        for (UUID userUUID: registeredUsers) {
            sb.append("\t").append(userUUID.toString()).append("\n");
        }
        sb.append("Participants num: ").append(participantsNum).append("\n");
        sb.append("Deadline: ").append(deadline).append("\n");
        sb.append("Location: ").append(location).append("\n");
        sb.append("Start date: ").append(startDate).append("\n");
        sb.append("Duration: ").append(duration).append("\n");
        sb.append("Cost: ").append(cost).append("\n");
        sb.append("In Quota: ").append(inQuota).append("\n");
        sb.append("End Date: ").append(endDate).append("\n");
        sb.append("Notes: ").append(notes).append("\n");
        return sb.toString();
    }
}
