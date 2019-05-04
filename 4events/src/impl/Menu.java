package impl;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.nio.file.Path;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

class Menu {
    private static final String MENU_JSON_PATH = "res/IT_MenuDescr.json";

    private Connector myConnector;
    private jsonTranslator menuTranslation;

    /**
     *
     * @param dbConnector a impl. Connector to the local database
     */
    Menu (Connector dbConnector) {
        this.myConnector = dbConnector;
        Path menuJsonPath = Paths.get(MENU_JSON_PATH);
        menuTranslation = new jsonTranslator(menuJsonPath.toString());
    }

    void printWelcome() {
        System.out.println(menuTranslation.getTranslation("welcome"));
    }

    void printExit() {
        System.out.println(menuTranslation.getTranslation("exit"));
    }

    /**
     * Fills all the fields of a given impl.Event object
     * //TODO Finish this insertion method
     * @throws IllegalStateException if user input is logically inconsistent (start date after end date and so on)
     */
    void fillEventFields(Event event) throws IllegalStateException {
        EventFactory factory = new EventFactory(myConnector);
        jsonTranslator eventJson = new jsonTranslator(Event.getJsonPath());

        for (String eventType: myConnector.getCategories()) {
            Event game = factory.createEvent(eventType);
            System.out.println(game.getCatName() + '\n');

            LinkedHashMap<String, Class<?>> eventFieldsMap = game.getFields();

            Iterator iterator = eventFieldsMap.entrySet().iterator(); // Get an iterator for our map

            while(iterator.hasNext()) {
                Map.Entry entry = (Map.Entry)iterator.next(); // Casts the iterated item to a Map Entry to use it as such
                String inputDescription = eventJson.getName( (String)entry.getKey() );
                Object userInput = InputManager.genericInput(inputDescription, (Class)entry.getValue());
                if (userInput == null)
                    return; // TODO This should probably be changed to an Exception
                else {
                    event.setAttribute( (String)entry.getKey(), userInput);
                }
            }

            event.isLegal();
        }
    }

    /**
     * Prints name and description of available categories' fields
     */
    void printFieldsName() {
        System.out.println(menuTranslation.getTranslation("categoryList"));

        EventFactory factory = new EventFactory(myConnector);
        jsonTranslator eventJson = new jsonTranslator(Event.getJsonPath());

        for (String eventType: myConnector.getCategories()) {
            Event game = factory.createEvent(eventType);
            System.out.println(game.getCatName() + "\n  " + game.getCatDescription() + '\n');

            int maxLength = 0;

            for (String field : game.getFieldsName()) { // Traverse all the names and...
                int length = eventJson.getName(field).length();
                if (length > maxLength)
                    maxLength = length; // ...find the longest
            }
            maxLength += 3; // Add some more char to allow spacing between the longest name and its description

            for (String field : game.getFieldsName()) {
                StringBuffer outputBuffer = new StringBuffer();
                outputBuffer.append("  ");
                outputBuffer.append(eventJson.getName(field));
                outputBuffer.append(':');
                for (int i = 0; i < (maxLength - eventJson.getName(field).length()); i++) { // Wonderful wizardry
                    outputBuffer.append(" "); // For spacing purposes
                }
                outputBuffer.append(eventJson.getDescr(field));

                System.out.println(outputBuffer);
            }
        }
    }

    /**
     * Nested class that's used to store the JSONObject representation of a translation on disk.
     */
    class jsonTranslator {
        JSONObject jsonContent;

        /**
         * Instantiate a jsonTranslator object with the given json file
         * @param jsonPath Path to the json file to load
         */
        jsonTranslator (String jsonPath) {
            InputStream inputStream = getClass().getResourceAsStream(jsonPath); // Tries to open the json as a resource
            if (inputStream == null) // If getResourceAsStream returns null, we're not running in a jar
                try {
                    inputStream = new FileInputStream(jsonPath); // Then we need to read the file from disk
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

            JSONTokener tokener = new JSONTokener(inputStream);
            jsonContent = new JSONObject(tokener);
        }

        /**
         * Translates a field to a human readable text
         * @param key The key to search for in json file
         * @return <String> The string corresponding to key
         */
        String getTranslation (String key) {
            try {
                return jsonContent.getString(key);
            } catch (JSONException e) {
                return ("ALERT: Missing element in json file: " + key);
            }
        }

        /**
         * Translates a field to a human readable text
         * @param key The key to search for in json file
         * @return <String> The name corresponding to key
         */
        String getName (String key) {
            try {
                return jsonContent.getJSONObject(key).getString("name");
            } catch (JSONException e) {
                return ("ALERT: Missing element in json file: " + key);
            }
        }

        /**
         * Translates a field to a human readable description
         * @param key The key to search for in json file
         * @return <String> The description corresponding to key
         */
        String getDescr (String key) {
            try {
                return jsonContent.getJSONObject(key).getString("descr");
            } catch (JSONException e) {
                return ("ALERT: Missing element in json file: " + key);
            }
        }
    }
}
