package it.unibs.ing.se.view.inputwrappers;

import it.unibs.ing.se.model.fields.Sex;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedHashMap;

public class SoccerGameInput extends EventInput {
    public Sex      gender;
    public Integer  ageMin;
    public Integer  ageMax;

    private static final String eventType = "soccer_game";
    private final String[] mandatoryFields = {"gender", "ageMin", "ageMax"};

    static String getClassEventType() { return eventType; } // Static method to know how all the events of this type are saved in the DB

    SoccerGameInput(){
        super(eventType);
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



    @Override
    public String toString() {
        return super.toString() +
                "SoccerGameInput{" +
                "gender=" + gender +
                ", ageMin=" + ageMin +
                ", ageMax=" + ageMax +
                ", mandatoryFields=" + Arrays.toString(mandatoryFields) +
                '}';
    }
}
