package interfaces;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public interface ReflectionInterface {
    /**
     * A method to set a class' attribute to a given object passed from the caller
     */
    void setAttribute(String fieldName, Object content);

    /**
     * A method to get the names of public fields of a class and its fathers
     * @return an ArrayList of Strings
     */
    ArrayList<String> getAttributesName();

    /**
     * A method to get the fields of a class and its fathers
     * @return a LinkedHashMap with a String as a key and a Class<?> as Value
     *   - Key is field's name as a String (such as the one returned from getAttributesName)
     *   - Value is the internal type of the field
     */
    LinkedHashMap<String, Class<?>> getAttributes();

    /**
     * A method to get DB-input ready values of all non-null fields of a class and its fathers
     * @return a LinkedHashMap with a String as a key and an Object as Value
     *   - Key is field's name as a String (such as the one returned from getAttributesName)
     *   - Value is an Object with current value of a field
     */
    LinkedHashMap<String, Object> getNonNullAttributesWithValue();
}
