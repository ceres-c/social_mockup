package impl;

import impl.fields.MyDuration;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Calendar;
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
        VALID,
        OPEN,
        CLOSED,
        FAILED,
        ENDED
    }

    private static final String EVENT_JSON_PATH = "res/IT_EventDescr.json";

    private UUID        eventID; // UUID of the event stored in the DB
    private UUID        creatorID; // UUID of the user who created the event
    private String      category;
    private String      catName;
    private String      catDescription;
    public  String      title;
    public  Integer     partecipantsNum;
    public  Calendar    deadline;
    public  String      location;
    public  Calendar    startDate;
    public  MyDuration  duration; // Object used to store a time difference between two Calendars
    public  Double      cost;
    public  String      inQuota;
    public  Calendar    endDate;
    public  String      notes;

    Event(String catName, String catDescription) {
        this.catName = catName;
        this.catDescription = catDescription;
        eventID = UUID.randomUUID();
    }

    String getCatName() { return catName; }

    String getCatDescription() { return catDescription; }

    static String getJsonPath () {
        return EVENT_JSON_PATH;
    }

    /**
     * A method to get the fields of a class and its fathers
     * @return a LinkedHashMap with a String as a key and a Class<?> as Value
     *   - Key is field's name as a String (such as the one returned from getFieldsName)
     *   - Value is the internal type of the field
     */
    LinkedHashMap<String, Class<?>> getFields(){
        Field[] superFields = this.getClass().getSuperclass().getFields(); // Only public fields
        Field[] currentFields = this.getClass().getDeclaredFields(); // Both public and private fields

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
        for (Field field:currentFields) {
            if (field.getModifiers() == Modifier.PUBLIC) { // Filter out only public fields
                // Same goes for this line
                String fieldName = field.toString().substring( field.toString().lastIndexOf('.') + 1 );
                returnFields.put(fieldName, field.getType());
            }
        }
        return returnFields;
    }


    /**
     * A method to get the names of public fields of a class and its fathers
     * @return an ArrayList of Strings
     */
    public ArrayList<String> getFieldsName(){

        Field[] superFields = this.getClass().getSuperclass().getFields(); // Only public fields
        Field[] currentFields = this.getClass().getDeclaredFields(); // Both public and private fields

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
        for (Field field:currentFields) {
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
     * A method to check if the values input by an user are logically valid, used before saving to the DB
     * @return boolean:
     *      - True if legal
     * @throws IllegalStateException if user input isn't legal
     */
    public boolean isLegal() throws IllegalStateException {
        Calendar currentDate = Calendar.getInstance();

        Calendar startPlusDuration = Calendar.getInstance(startDate.getTimeZone());
        startPlusDuration.setTime(startDate.getTime()); // Thanks Java, mutables are the way to go.
        startPlusDuration.add(Calendar.MINUTE, duration.sizeOf()); // Now set the damn date

        if (startDate.before(deadline)) throw new IllegalStateException ("ALERT: start date prior than specified deadline");
        if (currentDate.after(deadline)) throw new IllegalStateException ("ALERT: deadline in the past");
        if (endDate.before(startDate) && !endDate.equals(startDate)) throw new IllegalStateException ("ALERT: end date prior start date");
        if (endDate.before(startPlusDuration)) throw new IllegalStateException ("ALERT: end date comes before start date + duration");
        return true;
    }

    @Override
    public String toString () {
        StringBuilder sb = new StringBuilder();
        sb.append("Title: ").append(title).append("\n");
        sb.append("Partecipants num: ").append(partecipantsNum).append("\n");
        sb.append("Deadline: ").append(deadline.getTime()).append("\n");
        sb.append("Location: ").append(location).append("\n");
        sb.append("Start date: ").append(startDate.getTime()).append("\n");
        sb.append("Duration: ").append(duration.sizeOf()).append("\n");
        sb.append("Cost: ").append(cost).append("\n");
        sb.append("In Quota: ").append(inQuota).append("\n");
        sb.append("End Date: ").append(endDate.getTime()).append("\n");
        sb.append("Notes: ").append(notes).append("\n");
        return sb.toString();
    }
}
