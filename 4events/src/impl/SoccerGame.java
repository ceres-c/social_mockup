package impl;

import impl.fields.Sex;
import interfaces.LegalObject;
import interfaces.ReflectionInterface;

import java.lang.reflect.Field;
import java.util.UUID;

public class SoccerGame extends Event implements LegalObject, ReflectionInterface {
    public Sex      gender;
    public Integer  ageMin;
    public Integer  ageMax;

    private static final String eventTypeDB = "soccer_game";
    private final String[] mandatoryFields = {"gender", "ageMin", "ageMax"};

    public static final String getClassEventTypeDB() { return eventTypeDB; } // Static method to know how all the events of this type are saved in the DB

    /**
     * This empty constructor has to be used ONLY for dummy objects.
     * If used for Event Objects that will be manipulated, it WILL lead to NullPointerExceptions
     *
     * Here be dragons
     */
    public SoccerGame() {
    }

    SoccerGame(UUID eventID, UUID creatorID){
        super(eventID, creatorID, eventTypeDB);
    }

    /**
     * A method to set a field to a given object passed from the caller
     */
    public void setAttribute(String fieldName, Object content) {
        try {
            Field field = this.getClass().getDeclaredField(fieldName); // If the field is not owned by this class, then...
            field.set(this, content);
        } catch (NoSuchFieldException e) {
            super.setAttribute(fieldName, content); // ... Search for it in the super class
        } catch (IllegalAccessException e) {
            System.out.println("ALERT: Illegal access on field: " + fieldName);
            e.printStackTrace();
        }
    }

    /**
     * A method to save an UserID into the Event object to keep track of registered users
     * @param user A User object from whose the UserID will be taken
     * @return True if everything went smoothly
     * @throws IllegalStateException If the event has already reached maximum number of registered users
     * @throws IllegalArgumentException If anything goes wrong while registering the user (error in Exception message)
     *                                  i.e User is already registered or User's Sex is not appropriate for this event
     */
    public boolean register(User user) throws IllegalArgumentException, IllegalStateException {
        if (this.registeredUsers.size() == this.participantsNum) {
            throw new IllegalStateException("ALERT: Event" + this.getEventID().toString() + " has already reached max number of users");
        } else if (registeredUsers.contains(user.getUserID())) {
            throw new IllegalArgumentException("ALERT: User " + user.getUsername() + " is already registered to this event");
        } else if (!user.getGender().equals(this.gender)) {
            throw new IllegalArgumentException("ALERT: User " + user.getUsername() + " sex is not allowed to subscribe to this event");
        }
        registeredUsers.add(user.getUserID());
        return true;
    }

    /**
     * A method to save an UserID into the Event object that has to be used ONLY to restore an event from the database.
     * For normal operation use register(User user)
     * @param userID A UUID object
     * @return True if everything went smoothly
     * @throws IllegalStateException If the event has already reached maximum number of registered users
     * @throws IllegalArgumentException If anything goes wrong while registering the user (error in Exception message)
     *                                  i.e User is already registered
     */
    public boolean register(UUID userID) throws IllegalArgumentException {
        if (registeredUsers.contains(userID)) {
            throw new IllegalArgumentException("ALERT: User " + userID.toString() + " is already registered to this event");
        }
        registeredUsers.add(userID);
        return true;
    }

    /**
     * A method to check if a field is mandatory or optional
     */
    public boolean isOptional(String fieldName) {
        if (! super.isOptional(fieldName)) return false;
        for (String field:mandatoryFields)
            if (fieldName.equals(field)) return false;
        return true;
    }

    /**
     * A method to check if the values input by an user are logically valid, used before saving to the database
     * @return boolean:
     *      - True if legal
     * @throws IllegalStateException if user input isn't legal
     */
    public boolean isLegal() throws IllegalStateException {
        super.isLegal();
        if (ageMax < ageMin) throw new IllegalStateException ("ALERT: min age higher than max age");
        return true;
    }

    @Override
    public String toString () {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append("Sex: ").append(gender == null ? "null" : gender.toString()).append("\n"); // If gender hasn't been set this avoids NullPointerExceptions
        sb.append("Age Min: ").append(ageMin).append("\n");
        sb.append("Age Max: ").append(ageMax).append("\n");
        return sb.toString();
    }
}
