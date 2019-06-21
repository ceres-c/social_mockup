package it.unibs.ing.se.model;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.SQLException;
import java.time.Duration;
import java.util.*;
import java.time.LocalDateTime;

import it.unibs.ing.se.DMO.Connector;
import it.unibs.ing.se.DMO.JsonTranslator;
import it.unibs.ing.se.model.fields.OptionalCost;

/**
 * This abstract class represents generic events that will be inflected
 * into specific categories via extending classes
 */
public abstract class Event implements LegalObject, ReflectionInterface {
    public enum State {
        UNKNOWN,
        VALID,
        OPEN,
        CLOSED,
        FAILED,
        ENDED,
        WITHDRAWN
    }

    private UUID            eventID; // UUID of the event stored in the DB
    private UUID            creatorID; // UUID of the user who created the event
    private String          eventType; // As present in DB category list: ie. "soccer_game"
    private boolean         published;
    private ArrayList<UUID> registeredUsers;
    private State           currentState;
    private int             participantsMax;
    /* Following fields are public to emphasize the fact that are user-controlled */
    public  String          title;
    public  Integer         participantsMin;
    public  Integer         participantsSurplus;
    public  LocalDateTime   registrationDeadline;
    public  String          location;
    public  LocalDateTime   startDate;
    public  Duration        duration;
    public  LocalDateTime   deregistrationDeadline;
    public  Double          cost;
    public  String          inQuota;
    public  LocalDateTime   endDate;
    public  String          notes;

    private final String[] mandatoryFields = {"participantsMin", "registrationDeadline", "location", "startDate", "cost"};

    /**
     * This empty constructor has to be used ONLY for dummy objects, such as those used to print field names in help section.
     * If used for Event Objects that will be manipulated, it WILL lead to NullPointerExceptions
     *
     * Here be dragons
     */
    public Event() { }

    public Event(UUID eventID, UUID creatorID, String eventType) {
        this.eventID = eventID;
        this.creatorID = creatorID;
        this.eventType = eventType;
        this.published = false;
        this.currentState = State.UNKNOWN;
        this.registeredUsers = new ArrayList<>();
    }

    public UUID getEventID() { return eventID; }

    public String getEventIDAsString() { return eventID.toString(); }

    public String getTitle() { return title; }

    public UUID getCreatorID() { return creatorID; }

    public String getCreatorIDAsString() { return creatorID.toString(); }

    public String getEventType() { return eventType; }

    public boolean isPublished () { return published; }

    public LocalDateTime getRegistrationDeadline() { return registrationDeadline; }

    public LocalDateTime getStartDate() { return startDate; }

    public ArrayList<UUID> getRegisteredUsers() { return registeredUsers; }

    public Double getCost() { return cost; }

    /**
     * Gets the available OptionalCosts for this event
     * Should return null if the event has not OptionalCosts
     * @return a LinkedHashMap with a String as a key and a OptionalCost as Value
     *                          - Key is costs's name as a String (such as the one returned from getAttributesName)
     *                            i.e. "lodge" for MountainHiking
     *                          - Value is a OptionalField object
     */
    public abstract LinkedHashMap<String, OptionalCost> getOptionalCosts();

    /**
     * Gets the available OptionalCosts for this event
     * Should return null if the event has not OptionalCosts
     * @return a LinkedHashMap with a String as a key and a OptionalCost as Value
     *                          - Key is costs's UUID
     *                          - Value is a OptionalField object
     */
    public abstract LinkedHashMap<UUID, Integer> getOptionalCostsByUUID();

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

    /**
     * Calculate total cost given an ArrayList of optionalCosts
     * @param wantedCosts ArrayList of UUID of OptionalCost objects
     * @return A double with the sum of all costs
     */
    public Double totalCost(ArrayList<UUID> wantedCosts) {
        Double totalCost = this.cost;
        LinkedHashMap<UUID, Integer> optionalCosts = this.getOptionalCostsByUUID();
        if (optionalCosts == null) {
            return totalCost;
        }
        for (UUID costID : wantedCosts) {
            if (!optionalCosts.containsKey(costID))
                throw new IllegalArgumentException("ALERT: Wanted cost " + costID + " is not present in event's available costs"); // This should never happen (TM)
            totalCost += optionalCosts.get(costID);
        }
        return totalCost;
    }

    public int registeredUsersCount() { return registeredUsers == null ? 0 : registeredUsers.size(); }

    public boolean userIDAlreadyRegistered(UUID userID) { return registeredUsers != null && registeredUsers.contains(userID); }

    /**
     * A method to save an UserID into the Event object to keep track of registered users
     * @param user A User object from which the UserID will be taken
     * @return True if registration was successful
     * @throws IllegalStateException If the event has already reached maximum number of registered users
     * @throws IllegalArgumentException If anything goes wrong while registering the user (error in Exception message)
     *                                  i.e User is already registered or User's Sex is not appropriate for this event
     */
    public boolean register(User user) throws IllegalArgumentException, IllegalStateException {
        if (this.registeredUsersCount() == this.getParticipantsMax()) {
            throw new IllegalStateException("ALERT: Event" + this.getEventID().toString() + " has already reached max number of users");
        } else if (this.userIDAlreadyRegistered(user.getUserID())) {
            throw new IllegalArgumentException("ALERT: User " + user.getUsername() + " is already registered to this event");
        }
        return registeredUsers.add(user.getUserID());
    }

    /**
     * A method to save a userID into the Event object that has to be used ONLY to restore an event from the database.
     * For normal operation use register(User user)
     * @param userID A UUID object
     * @return True if registration was successful
     */
    public boolean register(UUID userID) {
        if (this.userIDAlreadyRegistered(userID)) {
            throw new IllegalArgumentException("ALERT: User " + userID.toString() + " is already registered to this event");
        }
        return registeredUsers.add(userID);
    }

    /**
     * A method to remove a userID from the Event object so that a User can be unregistered
     * No need for overloaded method with UUID as a parameter since no deregistration action should generate from the database
     * @param user A User object from which the UserID will be taken
     * @return True if deregistration was succesful
     * @throws IllegalStateException If User was not registered
     * @throws IllegalArgumentException If the event has reached minimum number of registered users (0)
     */
    public boolean deregister(User user, LocalDateTime currentDateTime) throws IllegalArgumentException, IllegalStateException {
        if (this.registeredUsersCount() == 0) {
            throw new IllegalStateException("ALERT: Event" + this.getEventID().toString() + " has already reached min number of users (0)");
        } else if (!this.userIDAlreadyRegistered(user.getUserID())) {
            throw new IllegalArgumentException("ALERT: User " + user.getUsername() + " was not registered to this event");
        } else if (currentDateTime.isAfter(this.deregistrationDeadline)) {
            throw new IllegalStateException("ALERT: Can't deregister because deregistrationDeadline has passed");
        }
        return registeredUsers.remove(user.getUserID());
    }

    /**
     * This method empties out registeredUsers array and has to be used only if the Event enters withdrawn state
     */
    public void deregisterAll() {
        this.registeredUsers = new ArrayList<UUID>();
    }

    public State getCurrentState() { return currentState; }

    public String getCurrentStateAsString() { return this.currentState.name(); }

    /**
     * // TODO method description
     */
    public void setEventWithdrawn () {
        this.currentState = State.WITHDRAWN;
    } // TODO se lo stato non è OPEN, throw exception

    /**
     * Setter that has to be used ONLY to restore an event from the database. For normal operation use updateState.
     * @param state String version of a status from Event.State enum
     */
    public void setCurrentState (String state) { currentState = State.valueOf(state); }

    public int getParticipantsMax() { return participantsMax; }

    public void setParticipantsMax(int participantsMax) { this.participantsMax = participantsMax; }

    /**
     * A method to set an object as published.
     * updateState should be called afterwards to check for differences and update the database accordingly
     * @throws IllegalStateException If called on an event which is non-legal
     * @param currentDateTime
     */
    public void publish(LocalDateTime currentDateTime) throws IllegalStateException {
        if (!this.published && !isLegal(currentDateTime))
            throw new IllegalStateException("ALERT: Non legal events can't be published");
        this.published = true;
    }

    /**
     * Setter that has to be used ONLY to restore an event from the database. For normal operation use publish
     * @param state boolean of publication status
     */
    public void setPublished (boolean state) { this.published = state; }

    /**
     * Control the "Status" state machine basing on current Status and date/time.
     * @param currentDateTime LocalDateTime object with the date to check against if status has to be updated or not
     * @return True if Event's status has changed and then the event has to be updated in the database as well
     */
    public boolean updateState(LocalDateTime currentDateTime) {
        switch (currentState) {
            case UNKNOWN:
                if (this.isLegal(currentDateTime)) {
                    currentState = State.VALID;
                    return true;
                }
                break;

            case VALID:
                if (this.published && (currentDateTime.isBefore(registrationDeadline) || currentDateTime.equals(registrationDeadline))) {
                    currentState = State.OPEN;
                    return true;
                }
                break;

            case OPEN:
                if (currentDateTime.isBefore(registrationDeadline) || currentDateTime.equals(registrationDeadline)) {
                    if (registeredUsers.size() >= participantsMax) {
                        this.currentState = State.CLOSED;
                        return true;
                    }
                } else {
                    if (registeredUsers.size() < participantsMin) {
                        this.currentState = State.FAILED;
                    } else if (registeredUsers.size() >= participantsMin) {
                        this.currentState = State.CLOSED;
                    }
                    return true;
                }
                break;

            case CLOSED:
                if (registeredUsers.size() < participantsMax) {
                    // A user has withdrawn and the event is again open for new participants
                    this.currentState = State.OPEN;
                    return true;
                }
                if (endDate == null && duration == null) {
                    // We have no way to know how long an event will last
                    if (currentDateTime.isAfter(startDate.plusDays(1))) { // As of client request
                        this.currentState = State.ENDED;
                        return true;
                    }
                } else if (endDate == null) {
                    if (currentDateTime.isAfter(startDate.plus(duration))) {
                        // Start date + duration = end of event. If end of event is after current date, then the event has ended
                        this.currentState = State.ENDED;
                        return true;
                    }
                } else if (duration == null) {
                    if (currentDateTime.isAfter(endDate)) {
                        this.currentState = State.ENDED;
                        return true;
                    }
                }
                break;

            default:
                return false; // We're in ENDED FAILED or WITHDRAWN statuses and those can't be altered
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
     * @param currentDate
     */
    public boolean isLegal(LocalDateTime currentDate) throws IllegalStateException {

        if (startDate.isBefore(registrationDeadline))
            throw new IllegalStateException ("ALERT: start date prior than specified registrationDeadline");
        if (currentDate.isAfter(registrationDeadline))
            throw new IllegalStateException ("ALERT: registrationDeadline in the past");
        if (endDate != null && endDate.isBefore(startDate) && !endDate.equals(startDate))
            throw new IllegalStateException ("ALERT: end date prior start date");
        if (duration != null && endDate != null && endDate.isBefore(startDate.plus(duration)))
            throw new IllegalStateException("ALERT: end date comes before start date + duration");
        if (deregistrationDeadline != null && deregistrationDeadline.isAfter(registrationDeadline))
            throw new IllegalStateException("ALERT: deregistrationDeadline date comes after registrationDeadline");

        return true;
    }

    @Override
    public String toString () {
        StringBuilder sb = new StringBuilder();
        sb.append("Title: ").append(title).append("\n");
        sb.append("Creator ID: ").append(creatorID.toString()).append("\n");
        sb.append("Database Event Type: ").append(eventType).append("\n");
        sb.append("Published: ").append(published).append("\n");
        sb.append("Status: ").append(currentState.name()).append("\n");
        sb.append("Registered users: \n");
        for (UUID userUUID: registeredUsers) {
            sb.append("\t").append(userUUID.toString()).append("\n");
        }
        sb.append("Min participants: ").append(participantsMin).append("\n");
        sb.append("Surplus participants:").append(participantsSurplus).append('\n');
        sb.append("Max participants: ").append(participantsMax).append('\n');
        sb.append("Deadline: ").append(registrationDeadline).append("\n");
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
