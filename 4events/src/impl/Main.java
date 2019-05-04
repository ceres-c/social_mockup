package impl;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    private static final String CONFIG_JSON_PATH = "config.json";

    public static void main(String args[]) {
        Connector myConnector = null; // Declared null to shut the compiler up, on usage it will always be properly instanced
        Path configPath = Paths.get(CONFIG_JSON_PATH);
        jsonConfigReader config = new jsonConfigReader(configPath.toString());
        try {
            myConnector = new Connector(config.getDBURL(), config.getDBUser(), config.getDBPassword());
        } catch (IllegalStateException e) {
            System.exit(1); // Error is printed by the impl.Connector constructor
        }

        Menu menu = new Menu(myConnector);
        menu.printWelcome();

        //menu.printFieldsName(); // TODO use this function for "help" section, printing available fields
        // TODO REMOVE following testing code
        Event game = new SoccerGame("soccer_game", "Test description");
        menu.fillEventFields(game);
        // TODO REMOVE END

        try {
            myConnector.saveEventToDb(game); // TODO remove this testing code
        } catch (Exception e) {
            // LOL
        }

        myConnector.closeDb();
        menu.printExit();
    }

    /**
     * Nested class that's used to store the JSONObject representation of the configuration on disk.
     */
    static class jsonConfigReader {
        JSONObject jsonContent;

        /**
         * Initializes the config with a given json file
         * @param jsonPath Path to the json file to load
         */
        jsonConfigReader (String jsonPath) {
            try (InputStream inputStream = new FileInputStream(jsonPath) ) {
                // settings.json is always in the same path, so no need to check if we're in a jar or not
                JSONTokener tokener = new JSONTokener(inputStream);
                jsonContent = new JSONObject(tokener);

            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        String getDBURL () throws JSONException {
            return jsonContent.getString("db_url");
        }

        String getDBPassword () throws JSONException {
            return jsonContent.getString("db_password");
        }

        String getDBUser () throws JSONException {
            return jsonContent.getString("db_username");
        }
    }
}