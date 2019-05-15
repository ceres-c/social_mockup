package impl;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.nio.file.Path;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Contains all the methods used to interact with the user
 * Singleton
 */
class Menu {
    private static Menu singleInstance = null;

    private static final String MENU_JSON_PATH = "res/IT_MenuDescr.json";

    private Connector myConnector;
    private jsonTranslator menuTranslation;

    /**
     * Private constructor
     * @param dbConnector a impl. Connector to the local database
     */
    private Menu (Connector dbConnector) {
        this.myConnector = dbConnector;
        Path menuJsonPath = Paths.get(MENU_JSON_PATH);
        menuTranslation = new jsonTranslator(menuJsonPath.toString());
    }

    public static Menu getInstance(Connector dbConnector)
    {
        if (singleInstance == null)
            singleInstance = new Menu(dbConnector);

        return singleInstance;
    }

    void printWelcome() {
        System.out.println(menuTranslation.getTranslation("welcome"));
    }

    void printExit() {
        System.out.println(menuTranslation.getTranslation("exit"));
    }

    /**
     * Prompts the user to choose between login and signup
     * @return true if the user wants to login to an already existing account
     */
    boolean loginOrSignup() {
        Integer userInput = 0;
        while (userInput != 1 && userInput != 2) {
            userInput = InputManager.inputInteger(menuTranslation.getTranslation("loginOrSignup"));
            userInput = (userInput == null ? 0 : userInput); // NullObjectExceptions are FUN
        }
        return userInput == 1;
    }

    /**
     * Performs a user login operation
     * @return User object - Could be null if login fails (wrong username/password)
     */
    User login() throws SQLException {
        String username;
        char[] password;
        while ((username = InputManager.inputString("Username")) == null);
        while ((password = InputManager.inputPassword("Password")) == null);

        byte[] salt = charArrayToByteArray(username.toCharArray());
        String hashedPassword = SHA512PasswordHash(password, salt);
        java.util.Arrays.fill(password, ' '); // It will still be somewhere in memory due to Java's Almighty Garbage Collector (TM), but at least we tried.

        User userFromDb = new User();
        try {
            userFromDb = myConnector.getUser(username, hashedPassword);
        } catch (IllegalArgumentException e) {
            System.err.println(menuTranslation.getTranslation("loginError"));
            return null; // CHECK FOR NULL-OBJECT!
        }
        return userFromDb;
    }

    /**
     * Performs a user signup operation
     * @return User object - Could be null if signup fails (username already present in database)
     */
    User signup() throws SQLException {
        String username;
        char[] password;
        while ((username = InputManager.inputString("Username")) == null);
        while ((password = InputManager.inputPassword("Password")) == null);

        byte[] salt = charArrayToByteArray(username.toCharArray());
        String hashedPassword = SHA512PasswordHash(password, salt);
        java.util.Arrays.fill(password, ' '); // It will still be somewhere in memory due to Java's Almighty Garbage Collector (TM), but at least we tried.

        User newUser = new User(username, hashedPassword);
        try {
            myConnector.insertUser(newUser);
        } catch (IllegalArgumentException e) {
            System.err.println(menuTranslation.getTranslation("duplicateUser"));
            return null; // CHECK FOR NULL-OBJECT!
        }
        return newUser;
    }

    /**
     * Fills all the fields of a given impl.Event object
     * @throws IllegalStateException if user input is logically inconsistent (start date after end date and so on)
     */
    void fillEventFields(Event event) throws IllegalStateException {
        jsonTranslator eventJson = new jsonTranslator(Event.getJsonPath());

        LinkedHashMap<String, Class<?>> eventFieldsMap = event.getAttributesWithType();

        Iterator iterator = eventFieldsMap.entrySet().iterator(); // Get an iterator for our map

        while(iterator.hasNext()) {
            Map.Entry entry = (Map.Entry)iterator.next(); // Casts the iterated item to a Map Entry to use it as such

            boolean validUserInput = false;
            do {
                String inputDescription = eventJson.getName((String) entry.getKey());
                Object userInput = InputManager.genericInput(inputDescription, (Class) entry.getValue());
                if (userInput == null) {
                    validUserInput = event.isOptional((String) entry.getKey());
                } else {
                    event.setAttribute((String) entry.getKey(), userInput);
                    validUserInput = true;
                }
            } while (!validUserInput);
        }

        event.isLegal();
    }

    /**
     * Prints name and description of available categories' fields
     */
    void printFieldsName(Event event) {
        System.out.println(menuTranslation.getTranslation("categoryList"));
        jsonTranslator eventJson = new jsonTranslator(Event.getJsonPath());

        ArrayList<String> catDescription = myConnector.getCategoryDescription(event.getEventID());
        System.out.println(catDescription.get(0) + "\n  " + catDescription.get(1) + '\n'); // TODO the description could be moved to a json file

        int maxLength = 0;

        for (String field : event.getAttributesName()) { // Traverse all the names and...
            int length = eventJson.getName(field).length();
            if (length > maxLength)
                maxLength = length; // ...find the longest
        }
        maxLength += 3; // Add some more char to allow spacing between the longest name and its description

        for (String field : event.getAttributesName()) {
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

    /**
     * Hashes a password with SHA512 and given salt
     * @param password A char array to hash
     * @param salt A byte array to salt the password with
     * @return A String result of salt + hashing operation
     */
    private static String SHA512PasswordHash(char[] password, byte[] salt) {
        byte[] byteArrayPassword = charArrayToByteArray(password);
        String generatedPassword = null; // Just to shut the compiler up, this variable WILL be initialized once we return
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            md.update(salt);
            byte[] bytes = md.digest(byteArrayPassword);
            StringBuilder sb = new StringBuilder();
            for(int i=0; i < bytes.length; i++)
                sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
            generatedPassword = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            System.out.println("ALERT: Missing hashing algorithm SHA-512");
            e.printStackTrace();
        }
        java.util.Arrays.fill(byteArrayPassword, (byte)0x00); // It will still be somewhere in memory due to Java's Almighty Garbage Collector (TM), but at least we tried.
        return generatedPassword;
    }

    /**
     * Converts a char array to byte array to be used with Java hashing methods
     * @param charArray char array
     * @return A byte array representing our chars
     */
    private static byte[] charArrayToByteArray(char[] charArray) {
        byte[] byteArray = new byte[charArray.length];
        for(int i= 0; i < charArray.length; i++) {
            byteArray[i] = (byte)(0xFF & (int)charArray[i]);
        }
        return byteArray;
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
