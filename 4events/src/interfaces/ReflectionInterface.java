package interfaces;

import java.util.ArrayList;

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
}
