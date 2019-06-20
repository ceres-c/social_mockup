package it.unibs.ing.se.DMO;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Nested class that's used to store the JSONObject representation of a translation on disk.
 */
public class JsonTranslator {
    public static final String MENU_JSON_PATH = "res/IT_MenuDescr.json";
    public static final String EVENT_JSON_PATH = "res/IT_EventDescr.json";
    JSONObject jsonContent;

    /**
     * Instantiate a jsonTranslator object with the given json file
     * @param jsonPath Path to the json file to load
     */
    public JsonTranslator(String jsonPath) {

        String absPath = "/" + jsonPath; // Because java is fun and refers local files with a slash even on windows systems
        // Ask me about how I found that out.

        InputStream inputStream = getClass().getResourceAsStream(absPath); // Tries to open the json as a resource
        if (inputStream == null) // If getResourceAsStream returns null, we're not running in a jar
            try {
                Path localPath = Paths.get(jsonPath); // Adapt the path to current OS
                inputStream = new FileInputStream(localPath.toString()); // Then we need to read the file from disk
            } catch (IOException e1) {
                e1.printStackTrace();
            }

        JSONTokener tokener = new JSONTokener(inputStream);
        jsonContent = new JSONObject(tokener);
    }

    /**
     * Translates a field to human readable text
     * @param key The key to search for in json file
     * @return <String> The string corresponding to key
     */
    public String getTranslation (String key) {
        try {
            return jsonContent.getString(key);
        } catch (JSONException e) {
            return ("ALERT: Missing element in json file: " + key);
        }
    }

    /**
     * Translates a field to human readable text
     * @param key The key to search for in json file
     * @return <String> The name corresponding to key
     */
    public String getName (String key) {
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
    public String getDescr (String key) {
        try {
            return jsonContent.getJSONObject(key).getString("descr");
        } catch (JSONException e) {
            return ("ALERT: Missing element in json file: " + key);
        }
    }
}
