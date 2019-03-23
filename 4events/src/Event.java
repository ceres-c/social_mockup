import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.UUID;

/**
 * This abstract class represents generic events that will be inflected
 * into specific categories via extending classes
 */
abstract class Event {
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
    public  Calendar    duration; // Calendar object subsequent to startDate used to calculate a time interval
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
    ArrayList<String> getFieldsName(){

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

    public void setField(String fieldName, Object content) {
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

}
