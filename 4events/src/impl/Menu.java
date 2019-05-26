package impl;

import impl.fields.Sex;

import java.nio.file.Paths;
import java.nio.file.Path;

import java.sql.SQLException;
import java.util.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Contains all the methods used to interact with the user
 * Singleton
 */
class Menu {
    private static Menu singleInstance = null;

    static final String MENU_JSON_PATH = "res/IT_MenuDescr.json";

    private Connector myConnector;
    private Main.jsonTranslator menuTranslation;

    /**
     * Private constructor
     * @param dbConnector a Connector object already connected to the local database
     * @param menuTranslation a jsonTranslator object instantiated with menu json
     */
    private Menu (Connector dbConnector, Main.jsonTranslator menuTranslation) {
        this.myConnector = dbConnector;
        this.menuTranslation = menuTranslation;
    }

    static Menu getInstance(Connector dbConnector, Main.jsonTranslator menuTranslation)
    {
        if (singleInstance == null)
            singleInstance = new Menu(dbConnector, menuTranslation);

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
     * @return User object once login or signup have been completed
     */
    User loginOrSignup() {
        Integer userInput = 0;
        User returnUser = null;
        while (userInput != 1 && userInput != 2) {
            userInput = InputManager.inputInteger(menuTranslation.getTranslation("loginOrSignup"), true);
            userInput = (userInput == null ? 0 : userInput); // NullObjectExceptions are FUN
        }
        if (userInput == 1) {
            // Login
            try {
                returnUser = login();
            } catch (SQLException e) {
                System.err.println("FATAL: couldn't fetch user's data from the database. Contact your sysadmin.");
                e.printStackTrace();
                System.exit(1);
            }
        } else {
            // Sign Up
            try {
                returnUser = signup();
            } catch (SQLException e) {
                System.err.println("FATAL: couldn't add new user to the database. Contact your sysadmin.");
                e.printStackTrace();
                System.exit(1);
            }
        }
        return returnUser;
    }

    /**
     * Performs a user login operation
     * @return User object - Could be null if login fails (wrong username/password)
     */
    private User login() throws SQLException {
        String username;
        char[] password;
        while ((username = InputManager.inputString("Username", true)) == null);
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
    private User signup() throws SQLException {
        String username;
        char[] password;
        Sex    gender;
        while ((username = InputManager.inputString("Username", true)) == null);
        while ((password = InputManager.inputPassword("Password")) == null);
        gender = Sex.sexInput(menuTranslation.getTranslation("genderInput"), true);

        byte[] salt = charArrayToByteArray(username.toCharArray());
        String hashedPassword = SHA512PasswordHash(password, salt);
        java.util.Arrays.fill(password, ' '); // It will still be somewhere in memory due to Java's Almighty Garbage Collector (TM), but at least we tried.

        User newUser = new User(username, hashedPassword, gender);
        try {
            myConnector.insertUser(newUser);
        } catch (IllegalArgumentException e) {
            System.err.println(menuTranslation.getTranslation("duplicateUser"));
            return null; // CHECK FOR NULL-OBJECT!
        }
        return newUser;
    }

    /**
    * Prompts the user to choose among available functions of the software
    * @param user Current User object to fetch number of new notifications
    * @return A Main.Command enum type
    */
    Main.Command displayMainMenu(User user) {
        int unreadNotificationsNum;
        try {
            unreadNotificationsNum = myConnector.getUnreadNotificationsCountByUser(user);
        } catch (SQLException e) {
            return null;
        }
        String header = String.format(menuTranslation.getTranslation("mainMenuHeader"), user.getUsername(), unreadNotificationsNum);
        System.out.println(header);

        StringBuilder sb = new StringBuilder();
        sb.append("1) ").append(menuTranslation.getTranslation("mainMenuDashboard")).append('\n');
        sb.append("2) ").append(menuTranslation.getTranslation("mainMenuEventList")).append('\n');
        sb.append("3) ").append(menuTranslation.getTranslation("mainMenuCreateEvent")).append('\n');
        sb.append("4) ").append(menuTranslation.getTranslation("mainMenuHelp")).append('\n');
        sb.append("5) ").append(menuTranslation.getTranslation("mainMenuLogout")).append('\n');
        sb.append("6) ").append(menuTranslation.getTranslation("mainMenuQuit")).append('\n');

        System.out.println(sb);

        Integer userInput = InputManager.inputInteger(menuTranslation.getTranslation("userSelection"), false);

        Main.Command[] commands = Main.Command.values();
        if (userInput == null ||userInput <= 0 || userInput > commands.length)
            return Main.Command.INVALID;

        return commands[userInput];
    }

    /**
     * Prints out all the available categories and their respective fields with a brief description
     */
    void displayHelp() {
        System.out.println(menuTranslation.getTranslation("categoryList"));

        ArrayList<String> categories = myConnector.getCategories();

        EventFactory eFactory = new EventFactory();
        Event event;

        for (int i = 0; i < categories.size(); i++) {
            String catName = categories.get(i);

            ArrayList<String> catDescription = getCategoryDescription(catName);
            System.out.println(catDescription.get(0) + " - " + catDescription.get(1));

            event = eFactory.createEvent(catName);
            printEventFieldsName(event);
        }
    }

    /**
     * Prompts the user to choose an event among public ones.
     * No checks are made to ensure the user is allowed to register to selected event (i.e.: female users can select events for males).
     * @return Event object of the selected event, null if user aborted selection.
     */
    Event chooseEventFromPublicList() {
        Path eventJsonPath = Paths.get(Event.getJsonPath());
        Main.jsonTranslator eventTranslation = new Main.jsonTranslator(eventJsonPath.toString());

        ArrayList<Event> eventsInDB = null;
        try {
            eventsInDB = myConnector.getOpenEvents();
            for (int i = 0; i < eventsInDB.size(); i++) {
                Event event = eventsInDB.get(i);
                System.out.println((i + 1) + ") " + event.synopsis(eventTranslation, myConnector));
            }
        } catch (SQLException e) {
            System.err.println(menuTranslation.getTranslation("SQLError"));
            e.printStackTrace();
            System.exit(1);
        } catch (NoSuchElementException ex) {
            System.out.println(menuTranslation.getTranslation("noEventsInDB"));
            return null;
        }

        Integer userSelection = InputManager.inputInteger(menuTranslation.getTranslation("selectEventToShow"), false);
        if (userSelection == null || userSelection <= 0 || userSelection > eventsInDB.size() + 1) {
            System.out.println(menuTranslation.getTranslation("invalidUserSelection"));
            return null;
        }
        return eventsInDB.get(userSelection - 1);
    }

    /**
     * Prompts the user to choose among available options in his dashboard
     * @return User choice
     */
    int displayDashboard() {
        StringBuilder sb = new StringBuilder();
        sb.append(menuTranslation.getTranslation("welcomeDashboard")).append('\n');
        sb.append("1) ").append(menuTranslation.getTranslation("showPersonalNotifications")).append('\n');
        sb.append("2) ").append(menuTranslation.getTranslation("showPersonalEvents")).append('\n');
        sb.append("3) ").append(menuTranslation.getTranslation("showRegisteredEvents")).append('\n');
        System.out.println(sb);

        Integer userSelection;
        do {
            userSelection = InputManager.inputInteger(menuTranslation.getTranslation("userSelection"), false);
        } while (userSelection == null || userSelection < 0 || userSelection > 3); // Update this check if adding new elements in the above stringbuilder

        return userSelection;
    }

    /**
     * Prints all user's notifications and allows to mark them as read
     * @param user Current User object to fetch notifications
     */
    void displayNotifications (User user) {
        ArrayList<Notification> notifications;
        try {
            notifications = myConnector.getAllNotificationsByUser(user);
        } catch (SQLException e) {
            System.err.println();
            return;
        }
        if (notifications.size() == 0) {
            System.out.println(menuTranslation.getTranslation("noPersonalNotifications"));
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(menuTranslation.getTranslation("welcomeNotification")).append('\n');

        String notificationRead = menuTranslation.getTranslation("notificationRead");
        String notificationUnread = menuTranslation.getTranslation("notificationUnread");
        int maxLen;
        if (notificationRead.length() > notificationUnread.length()) // Avoids fixed length to allow future translations
            maxLen = notificationRead.length();
        else
            maxLen = notificationUnread.length();

        String notificationReadSpacer = new String(new char[maxLen - notificationRead.length()]).replace('\0', ' ');
        String notificationUnreadSpacer = new String(new char[maxLen - notificationUnread.length()]).replace('\0', ' ');
        String descriptionSpacer = "     ";

        for (int i = 0; i < notifications.size(); i++) {
            Notification notification = notifications.get(i);
            String notificationStatus = notification.isRead() ? notificationRead : notificationUnread;
            String statusSpacer = notification.isRead() ? notificationReadSpacer : notificationUnreadSpacer;

            sb.append(i + 1).append(") ");
            sb.append(notificationStatus);
            sb.append(statusSpacer);
            sb.append(" - ");
            sb.append(notification.getTitle()).append('\n');
            sb.append(descriptionSpacer);
            sb.append(notification.getContent()).append('\n');
        }
        System.out.println(sb);

        Integer userSelection;
        while (true) {
            userSelection = InputManager.inputInteger(menuTranslation.getTranslation("selectNotificationToSetAsRead"), false);
            if (userSelection == null || userSelection <= 0 || userSelection > notifications.size() + 1) {
                System.out.println(menuTranslation.getTranslation("invalidUserSelection"));
                return;
            }
            Notification notification = notifications.get(userSelection - 1);
            notification.setRead(true);
            try {
                myConnector.updateNotificationRead(notification);
            } catch (SQLException e) {
                System.err.println(menuTranslation.getTranslation("errorSettingNotificationAsRead"));
            }
        }
    }

    /**
     * Display all the events an user has created
     * @param user Current User object to fetch events
     */
    void displayEventsByCreator(User user) {
        Path eventJsonPath = Paths.get(Event.getJsonPath());
        Main.jsonTranslator eventTranslation = new Main.jsonTranslator(eventJsonPath.toString());
        try {
            ArrayList<Event> eventsInDB = myConnector.getEventsByCreator(user);
            for (Event eventInDB: eventsInDB) {
                System.out.println(eventInDB.synopsis(eventTranslation, myConnector));
            }
        } catch (SQLException e) {
            System.err.println(menuTranslation.getTranslation("SQLError"));
            e.printStackTrace();
            System.exit(1);
        } catch (NoSuchElementException ex) {
            System.out.println(menuTranslation.getTranslation("noCreatedEvents"));
        }
    }

    /**
     * Display all the events an user has registered to
     * @param user Current User object to fetch events
     */
    void displayEventsByRegistration(User user) {
        Path eventJsonPath = Paths.get(Event.getJsonPath());
        Main.jsonTranslator eventTranslation = new Main.jsonTranslator(eventJsonPath.toString());
        try {
            ArrayList<Event> eventsInDB = myConnector.getEventsByRegistration(user);
            for (Event eventInDB: eventsInDB) {
                System.out.println(eventInDB.synopsis(eventTranslation, myConnector));
            }
        } catch (SQLException e) {
            System.err.println(menuTranslation.getTranslation("SQLError"));
            e.printStackTrace();
            System.exit(1);
        } catch (NoSuchElementException ex) {
            System.out.println(menuTranslation.getTranslation("noRegisteredEvents"));
        }
    }

    /**
     * Prompt the user a list of available categories.
     * Once the user has selected the one he wants, proceeds to create an object of the right kind and fill its fields.
     * If the user chooses 0 or a number bigger than the number of available categories, null is returned.
     * @param user Current user who will be the creator of the Event
     * @return Event object of the right sub-class with required fields compiled - WARNING: can be null!
     *
     */
    Event createEvent(User user) throws IllegalStateException {
        EventFactory eFactory = new EventFactory();
        Event event = null;
        StringBuilder sb = new StringBuilder();

        sb.append(menuTranslation.getTranslation("categoryList")).append('\n');
        ArrayList<String> categories = myConnector.getCategories();
        for (int i = 0; i < categories.size(); i++) {
            ArrayList<String> catDescription = getCategoryDescription(categories.get(i));
            // Numbers printed below will be 1 based, so to select the right category user input hast to be decremented by one
            sb.append(i + 1).append(") ").append(catDescription.get(0)).append("\n     ").append(catDescription.get(1)).append('\n');
        }
        System.out.println(sb);

        Integer userSelection = InputManager.inputInteger(menuTranslation.getTranslation("userSelection"), false);
        if (userSelection == null || userSelection <= 0 || userSelection > categories.size() + 1) {
            System.out.println(menuTranslation.getTranslation("invalidUserSelection"));
            return null;
        }

        event = eFactory.createEvent(UUID.randomUUID(), user.getUserID(), categories.get(userSelection - 1));

        fillEventFields(event);

        return event;
    }

    /**
     * Asks the user to choose if he wants to register to an Event.
     * Useful only for user interaction, no information about the event itself is actually needed for this method.
     * @return true if the user wants to register, false otherwise
     *
     */
    boolean registerEvent() {
        Character userInput = null;

        do {
            userInput = InputManager.inputChar(menuTranslation.getTranslation("eventRegistration"), true);
            if (userInput != null && userInput != 'S' && userInput != 'N')
                userInput = null;
        } while (userInput == null);
        return userInput == 'S';
    }

    /**
     * Asks the user to choose if he wants the Event to be published in the Event List.
     * Useful only for user interaction, no information about the event itself is actually needed for this method.
     * @return true if the event has to be published, false otherwise
     *
     */
    boolean publishEvent() {
        Character userInput = null;

        do {
            userInput = InputManager.inputChar(menuTranslation.getTranslation("eventPublication"), true);
            if (userInput != null && userInput != 'S' && userInput != 'N')
                userInput = null;
        } while (userInput == null);
        return userInput == 'S';
    }

    /**
     * Fills all the fields of a given impl.Event object
     * @throws IllegalStateException if user input is logically inconsistent (start date after end date and so on)
     */
    void fillEventFields(Event event) throws IllegalStateException {
        Path eventJsonPath = Paths.get(Event.getJsonPath());
        Main.jsonTranslator eventTranslation = new Main.jsonTranslator(eventJsonPath.toString());

        LinkedHashMap<String, Class<?>> eventFieldsMap = event.getAttributesWithType();

        Iterator iterator = eventFieldsMap.entrySet().iterator(); // Get an iterator for our map

        while(iterator.hasNext()) {
            Map.Entry entry = (Map.Entry)iterator.next(); // Casts the iterated item to a Map Entry to use it as such

            boolean validUserInput = false;
            do {
                String inputDescription = eventTranslation.getName((String) entry.getKey());
                Object userInput = InputManager.genericInput(inputDescription, (Class) entry.getValue(), true);
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
    private void printEventFieldsName(Event event) {
        Path eventJsonPath = Paths.get(Event.getJsonPath());
        Main.jsonTranslator eventTranslation = new Main.jsonTranslator(eventJsonPath.toString());

        int maxLength = 0;

        for (String field : event.getAttributesName()) { // Traverse all the names and...
            int length = eventTranslation.getName(field).length();
            if (length > maxLength)
                maxLength = length; // ...find the longest
        }
        maxLength += 3; // Add some more char to allow spacing between the longest name and its description

        StringBuffer outputBuffer = new StringBuffer();

        for (String field : event.getAttributesName()) {
            outputBuffer.append("  ");
            outputBuffer.append(eventTranslation.getName(field));
            outputBuffer.append(':');
            for (int i = 0; i < (maxLength - eventTranslation.getName(field).length()); i++) { // Wonderful onelined math...
                outputBuffer.append(" "); // ...for spacing purposes
            }
            outputBuffer.append(eventTranslation.getDescr(field)).append('\n');
        }
        System.out.println(outputBuffer);
    }

    /**
     * Get a category's name and description given its internal name (eventType in db)
     * @return  an ArrayList of String.
     *              - 1st element: Category's full name
     *              - 2nd element: Category's description
     *          If the category does not exist, empty ArrayList.
     */
    private ArrayList<String> getCategoryDescription(String eventType) {
        Path eventJsonPath = Paths.get(Event.getJsonPath());
        Main.jsonTranslator eventTranslation = new Main.jsonTranslator(eventJsonPath.toString());

        ArrayList<String> returnCatDescr = new ArrayList<>();

        returnCatDescr.add(0, eventTranslation.getName(eventType));
        returnCatDescr.add(1, eventTranslation.getDescr(eventType));

        return returnCatDescr;
    }

    /**
     * Hashes a password with SHA512 and given salt
     * @param password A char array to hash
     * @param salt A byte array to salt the password with
     * @return A String result of salt + hashing operation
     */
    static String SHA512PasswordHash(char[] password, byte[] salt) {
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
    static byte[] charArrayToByteArray(char[] charArray) {
        byte[] byteArray = new byte[charArray.length];
        for(int i= 0; i < charArray.length; i++) {
            byteArray[i] = (byte)(0xFF & (int)charArray[i]);
        }
        return byteArray;
    }
}
