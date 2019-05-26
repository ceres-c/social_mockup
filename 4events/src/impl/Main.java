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

public class Main {
    public enum Command {
        INVALID,
        DASHBOARD,
        PUBLIC_EVENTS_LIST,
        NEW_EVENT,
        HELP,
        LOGOUT,
        QUIT
    }

    private static final String CONFIG_JSON_PATH = "config.json";

    public static void main(String args[]) {
        Connector myConnector = null; // Declared null to shut the compiler up, on usage it will always be properly instanced

        Path configJsonPath = Paths.get(CONFIG_JSON_PATH);
        jsonConfigReader config = new jsonConfigReader(configJsonPath.toString());

        Path menuJsonPath = Paths.get(Menu.MENU_JSON_PATH);
        jsonTranslator menuTranslation = new jsonTranslator(menuJsonPath.toString());

        try {
            myConnector = new Connector(config.getDBURL(), config.getDBUser(), config.getDBPassword());
        } catch (IllegalStateException e) {
            System.exit(1); // Error is printed by the impl.Connector constructor
        }

        // TODO update events status and throw notifications

        Menu menu = Menu.getInstance(myConnector, menuTranslation);
        menu.printWelcome();

        LocalDateTime currentDateTime = LocalDateTime.now();
        User currentUser;
        while ((currentUser = menu.loginOrSignup()) == null);

        Command userSelection = Command.INVALID;

        while (true) {
            userSelection = menu.displayMainMenu(currentUser);
            switch (userSelection) {
                case INVALID:
                    break;
                case DASHBOARD:
                    int dashboardUserSelection = menu.displayDashboard();
                    switch (dashboardUserSelection) {
                        case 1:
                            // Show Personal Notifications
                            menu.displayNotifications(currentUser);
                            break;
                        case 2:
                            // Show Events created by currentUSer
                            menu.displayEventsByCreator(currentUser);
                            break;
                        case 3:
                            // Show Events currentUSer has registered to
                            menu.displayEventsByRegistration(currentUser);
                            break;
                        default:
                            break;
                    }
                    break;
                case PUBLIC_EVENTS_LIST: {
                    Event existingEvent;
                    existingEvent = menu.chooseEventFromPublicList();

                    if (existingEvent != null) {
                        // Means the user has selected an event from the list
                        Path eventJsonPath = Paths.get(Event.getJsonPath());
                        Main.jsonTranslator eventTranslation = new Main.jsonTranslator(eventJsonPath.toString());

                        System.out.println(existingEvent.detailedDescription(eventTranslation, myConnector));

                        if (menu.registerEvent()) {
                            // The user wants to register
                            boolean canRegister = false;
                            try {
                                canRegister = existingEvent.register(currentUser);
                            } catch (IllegalArgumentException ex) {
                                if (ex.getMessage().contains("is already registered")) {
                                    System.err.println(menuTranslation.getTranslation("userAlreadyRegisteredToEvent"));
                                } else if (ex.getMessage().contains("sex is not allowed")) {
                                    System.err.println(menuTranslation.getTranslation("eventRegistrationSexMismatch"));
                                }
                            }

                            if (canRegister) {
                                try {
                                    myConnector.updateEventRegistrations(existingEvent);
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                    System.exit(1);
                                }
                            }
                        }
                    }

                    break;
                }
                case NEW_EVENT: {
                    Event newEvent = null;
                    boolean registerUser = false;

                    try {
                        newEvent = menu.createEvent(currentUser);
                    } catch (IllegalStateException e) {
                        System.err.println(menuTranslation.getTranslation("eventNotLegal"));
                        System.err.println(e.getMessage());
                        break;
                    }

                    if (newEvent == null) // User aborted event creation
                        break;

                    newEvent.updateStatus(currentDateTime); // UNKNOWN -> VALID
                    try {
                        myConnector.insertEvent(newEvent);
                    } catch (SQLException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }

                    try {
                        registerUser = newEvent.register(currentUser);
                    } catch (IllegalArgumentException ex) {
                        if (ex.getMessage().contains("is already registered")) {
                            System.err.println(menuTranslation.getTranslation("userAlreadyRegisteredToEvent"));
                        } else if (ex.getMessage().contains("sex is not allowed")) {
                            System.err.println(menuTranslation.getTranslation("eventCreationSexMismatch"));
                        }
                    }

                    if (registerUser) {
                        try {
                            myConnector.updateEventRegistrations(newEvent);
                        } catch (SQLException e) {
                            e.printStackTrace();
                            System.exit(1);
                        }
                    }

                    if (menu.publishEvent()) {
                        // The user wants to publish the event
                        try {
                            newEvent.publish();
                            newEvent.updateStatus(currentDateTime); // VALID -> OPEN
                            try {
                                myConnector.updateEventState(newEvent);
                                myConnector.updateEventPublished(newEvent);
                            } catch (SQLException e) {
                                e.printStackTrace();
                                System.exit(1);
                            }
                        } catch (IllegalStateException e) {
                            System.err.println(e.getMessage());
                            break;
                        }
                    }

                    break;
                }
                case HELP:
                    menu.displayHelp();
                    break;
                case LOGOUT:
                    while ((currentUser = menu.loginOrSignup()) == null); // Simply refreshes currentUser object
                    break;
                case QUIT:
                    myConnector.closeDb();
                    menu.printExit();
                    System.exit(0);
                default:
                    break;
            }
        }
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

    /**
     * Nested class that's used to store the JSONObject representation of a translation on disk.
     */
    static class jsonTranslator {
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
         * Translates a field to human readable text
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
         * Translates a field to human readable text
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
