package impl;

import impl.fields.Sex;
import interfaces.LegalObject;
import interfaces.ReflectionInterface;

import java.lang.reflect.Field;
import java.util.UUID;

public class SoccerGame extends Event implements LegalObject, ReflectionInterface {
    public Sex gender;
    public Integer  ageMin;
    public Integer  ageMax;

    private static final String catDB = "soccer_game";
    private final String[] mandatoryFields = {"gender", "ageMin", "ageMax"};

    public static final String getCatDBName() { return catDB; }

    SoccerGame(UUID creatorID, String catName, String catDescription){
        super(creatorID, catDB, catName, catDescription);
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
     * A method to check if a field is mandatory or optional
     */
    public boolean isOptional(String fieldName) {
        if (! super.isOptional(fieldName)) return false;
        for (String field:mandatoryFields)
            if (fieldName.equals(field)) return false;
        return true;
    }

    /**
     * A method to check if the values input by an user are logically valid, used before saving to the DB
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
        sb.append(super.toString()).append("\n");
        sb.append("Sex: ").append(gender.toString()).append("\n");
        sb.append("Age Min: ").append(ageMin).append("\n");
        sb.append("Age Max: ").append(ageMax).append("\n");
        return sb.toString();
    }
}
