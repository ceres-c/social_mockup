package it.unibs.ing.se.model;

import it.unibs.ing.se.model.fields.OptionalCost;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.UUID;

public class MountainHiking extends Event implements LegalObject, ReflectionInterface {
    public Integer  length;
    public Integer  heightDiff;
    public OptionalCost coach;
    public OptionalCost lodge;
    public OptionalCost lunch;

    private static final String eventType = "mountain_hiking";
    private final String[] mandatoryFields = {"length", "heightDiff"};

    static String getClassEventType() { return eventType; } // Static method to know how all the events of this type are saved in the DB

    /**
     * This empty constructor has to be used ONLY for dummy objects, such as those used to print field names in help section.
     * If used for Event Objects that will be manipulated, it WILL lead to NullPointerExceptions
     *
     * Here be dragons
     */
    MountainHiking() { }

    MountainHiking(UUID eventID, UUID creatorID){
        super(eventID, creatorID, eventType);
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
     * Gets the available OptionalCosts for this event
     * @return a LinkedHashMap with a String as a key and a OptionalCost as Value
     *                          - Key is costs's name as a String (such as the one returned from getAttributesName)
     *                            i.e. "lodge" for MountainHiking
     *                          - Value is a OptionalField object
     */
    public LinkedHashMap<String, OptionalCost> getOptionalCosts() {
        LinkedHashMap<String, OptionalCost> costsMap = new LinkedHashMap<>();
        if (coach != null) costsMap.put("coach", this.coach);
        if (lodge != null) costsMap.put("lodge", this.lodge);
        if (lunch != null) costsMap.put("lunch", this.lunch);
        if (costsMap.size() == 0) return null;
        return costsMap;
    }

    /**
     * Gets the available OptionalCosts for this event
     * @return a LinkedHashMap with a String as a key and a OptionalCost as Value
     *                          - Key is costs's UUID
     *                          - Value is a OptionalField object
     */
    public LinkedHashMap<UUID, Integer> getOptionalCostsByUUID() {
        LinkedHashMap<UUID, Integer> costsMap = new LinkedHashMap<>();
        if (coach != null) costsMap.put(this.coach.getCostID(), this.coach.getCostAmount());
        if (lodge != null) costsMap.put(this.lodge.getCostID(), this.lodge.getCostAmount());
        if (lunch != null) costsMap.put(this.lunch.getCostID(), this.lunch.getCostAmount());
        if (costsMap.size() == 0) return null;
        return costsMap;
    }

    /**
     * A method to check if the values input by an user are logically valid, used before saving to the database
     * @return boolean:
     *      - True if legal
     * @throws IllegalStateException if user input isn't legal
     * @param currentDate LocalDateTime object with the date to check against if status has to be updated or not
     */
    public boolean isLegal(LocalDateTime currentDate) throws IllegalStateException {
        super.isLegal(currentDate);
        if (length < 0) throw new IllegalStateException("ALERT: negative path length");
        if (coach != null && coach.getCostAmount() < 0) throw new IllegalStateException("ALERT: negative coach cost");
        if (lodge != null && lodge.getCostAmount() < 0) throw new IllegalStateException("ALERT: negative lodge cost");
        if (lunch != null && lunch.getCostAmount() < 0) throw new IllegalStateException("ALERT: negative lunch cost");
        return true;
    }

    @Override
    public String toString () {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append("Length: ").append(length).append("\n");
        sb.append("Height difference: ").append(heightDiff).append("\n");
        if (coach != null) sb.append("OptCost1 - Coach amount: ").append(coach.getCostAmount()).append("\n");
        if (lodge != null) sb.append("OptCost2 - Lodge amount: ").append(lodge.getCostAmount()).append("\n");
        if (lunch != null) sb.append("OptCost3 - Lunch amount: ").append(lunch.getCostAmount()).append("\n");
        return sb.toString();
    }
}
