package DMO;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Nested class that's used to store the JSONObject representation of the configuration on disk.
 */
public class JsonConfigReader {
    JSONObject jsonContent;

    /**
     * Initializes the config with a given json file
     * @param jsonPath Path to the json file to load
     */
    public JsonConfigReader(String jsonPath) {
        try (InputStream inputStream = new FileInputStream(jsonPath) ) {
            // settings.json is always in the same path, so no need to check if we're in a jar or not
            JSONTokener tokener = new JSONTokener(inputStream);
            jsonContent = new JSONObject(tokener);

        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public String getDBURL () throws JSONException {
        return jsonContent.getString("db_url");
    }

    public String getDBPassword () throws JSONException {
        return jsonContent.getString("db_password");
    }

    public String getDBUser () throws JSONException {
        return jsonContent.getString("db_username");
    }
}
