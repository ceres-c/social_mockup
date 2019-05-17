package impl;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.UUID;

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

        Menu menu = Menu.getInstance(myConnector);
        menu.printWelcome();

        User currentUser = null;
        LocalDateTime currentDateTime = LocalDateTime.now();

        while (currentUser == null) { // Login or signup are always needed before being able to use the software
            if (menu.loginOrSignup()) { // Prints whether to login or sign up
                // Login
                try {
                    currentUser = menu.login();
                } catch (SQLException e) {
                    System.err.println("Fatal error: couldn't fetch user's data from the database. Contact your sysadmin.");
                    e.printStackTrace();
                    System.exit(1);
                }
                if (currentUser != null) break;
            } else {
                // Sign Up
                try {
                    currentUser = menu.signup();
                } catch (SQLException e) {
                    System.err.println("FATAL: couldn't add new user to the database. Contact your sysadmin.");
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        }

        try {
            ArrayList<Event> events = myConnector.getEvents();
            for (Event event: events) {
                System.out.println(event);
            }
        } catch (SQLException e) {
            System.err.println("FATAL: couldn't read events from the database. Contact your sysadmin.");
            e.printStackTrace();
            System.exit(1);
        } catch (NoSuchElementException ex) {
            System.out.println("ALERT: No events currently in the database");
        }

        while (true) {
            //menu.printFieldsName(); // TODO use this function for "help" section, printing available fields
            // TODO REMOVE following testing code
            Event game = new SoccerGame(/* EventID */UUID.randomUUID(), currentUser.getUserID());
            menu.fillEventFields(game);

            if (game.isLegal()) {
                game.updateStatus(currentDateTime); // UNKNOWN -> VALID

                try {
                    myConnector.insertEvent(game);
                } catch (SQLException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            } else {

            }

            InputManager.inputString("Waiting for [ENTER] to publish the event");

            if (game.publish()) { // Publish the event
                game.updateStatus(currentDateTime); // VALID -> PUBLISHED
                try {
                    myConnector.updateEventPublished(game);
                } catch (SQLException e) {
                    e.printStackTrace();
                    System.exit(1);
                }

            }

            InputManager.inputString("Waiting for [ENTER] to register current user");

            if (game.register(currentUser)) { // Add user to registeredUsers
                try {
                    myConnector.updateEventRegistrations(game);
                } catch (SQLException e) {
                    e.printStackTrace();
                    System.exit(1);
                } // TODO catch IllegalArgumentException which happen when a user creates an event for different sex/age
            }// TODO REMOVE END
        }

        //myConnector.closeDb(); // TODO implement quit function in menu
        //menu.printExit();
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
