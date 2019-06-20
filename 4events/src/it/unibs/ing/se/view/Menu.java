package it.unibs.ing.se.view;

import it.unibs.ing.se.DMO.Connector;
import it.unibs.ing.se.DMO.JsonTranslator;
import it.unibs.ing.se.view.commands.MainCommand;
import it.unibs.ing.se.view.commands.DashboardCommand;
import it.unibs.ing.se.model.*;
import it.unibs.ing.se.model.fields.*;

import java.nio.file.Paths;
import java.nio.file.Path;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Contains all the methods used to interact with the user
 * Singleton
 */
public class Menu {
    private static Menu singleInstance = null;

    private Connector dbConnection;
    private JsonTranslator menuTranslation;

    /**
     * Private constructor
     * @param dbConnection a Connector object already connected to the local database
     * @param menuTranslation a jsonTranslator object instantiated with it.unibs.ing.se.view json
     */
    private Menu (Connector dbConnection, JsonTranslator menuTranslation) {
        this.dbConnection = dbConnection;
        this.menuTranslation = menuTranslation;
    }

    /**
     * Singleton method
     * @param dbConnection a Connector object already connected to the local database
     * @param menuTranslation a jsonTranslator object instantiated with it.unibs.ing.se.view json
     * @return
     */
    public static Menu getInstance(Connector dbConnection, JsonTranslator menuTranslation)
    {
        if (singleInstance == null)
            singleInstance = new Menu(dbConnection, menuTranslation);

        return singleInstance;
    }

    public void printExit() {
        System.out.println(menuTranslation.getTranslation("exit"));
    }

    /**
    * Prompts the user to choose among available functions of the software
    * @param user Current User object to fetch number of new notifications
    * @return A it.unibs.ing.se.Main.Command enum type
    */
    public MainCommand displayMainMenu(User user) {
        int unreadNotificationsNum;
        try {
            unreadNotificationsNum = dbConnection.getUnreadNotificationsCountByUser(user.getUserID());
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

        System.out.print(sb); // No trailing newline as it was already added on above line

        Integer userSelection = InputManager.inputInteger(menuTranslation.getTranslation("userSelection"), false);

        MainCommand[] mainCommands = MainCommand.values();
        if (userSelection == null ||userSelection <= 0 || userSelection >= mainCommands.length)
            return MainCommand.INVALID;

        return mainCommands[userSelection];
    }

    /**
     * Prints out all the available categories and their respective fields with a brief description
     */
    public void displayHelp() {
        System.out.println(menuTranslation.getTranslation("categoryList"));

        ArrayList<String> categories = dbConnection.getCategories();

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
    public Event chooseEventFromPublicList() {
        Path eventJsonPath = Paths.get(JsonTranslator.EVENT_JSON_PATH);
        JsonTranslator eventTranslation = new JsonTranslator(eventJsonPath.toString());

        ArrayList<Event> eventsInDB = null;
        try {
            eventsInDB = dbConnection.getOpenEvents();
            for (int i = 0; i < eventsInDB.size(); i++) {
                Event event = eventsInDB.get(i);
                System.out.println((i + 1) + ") " + event.synopsis(eventTranslation, dbConnection));
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
        if (userSelection == null || userSelection <= 0 || userSelection > eventsInDB.size()) {
            System.out.println(menuTranslation.getTranslation("invalidUserSelection"));
            return null;
        }
        return eventsInDB.get(userSelection - 1);
    }

    /**
     * Prompts the user to choose among available options in his dashboard
     * @return User choice
     */
    public DashboardCommand displayDashboard() {
        StringBuilder sb = new StringBuilder();
        sb.append(menuTranslation.getTranslation("welcomeDashboard")).append('\n');
        sb.append("1) ").append(menuTranslation.getTranslation("showUserProfile")).append('\n');
        sb.append("2) ").append(menuTranslation.getTranslation("showPersonalNotifications")).append('\n');
        sb.append("3) ").append(menuTranslation.getTranslation("showCreatedEvents")).append('\n');
        sb.append("4) ").append(menuTranslation.getTranslation("showRegisteredEvents")).append('\n');
        System.out.print(sb); // No trailing newline as it was already added on above line


        Integer userSelection = InputManager.inputInteger(menuTranslation.getTranslation("userSelection"), false);
        DashboardCommand[] dashCommands = DashboardCommand.values();
        if (userSelection == null || userSelection < 0 || userSelection >= dashCommands.length)
            return DashboardCommand.INVALID;

        return dashCommands[userSelection];
    }

    /**
     * Prints user profile and ask to change variable fields (Age and Favorite Categories)
     * @param currentUser User object to display data to
     * @return A new User object which might or might not be different from currentUser.
     *         UUID, username, password and Sex are retained.
     *         To know if the user edited other fields, the old currentUser object should be compared to this return.
     */
    public User displayAndEditUserProfile(User currentUser) {
        System.out.println(currentUser.detailedDescription(menuTranslation));
        Integer age = currentUser.getAge();
        String[] favoriteCategories = currentUser.getFavoriteCategories();
        Character userInput; // Will hold all the different user choices in questions below
        do {
            userInput = InputManager.inputChar(menuTranslation.getTranslation("ageChange"), true);
            if (userInput != null && userInput != 'S' && userInput != 'N')
                userInput = null;
        } while (userInput == null);
        if (userInput == 'S') {
            age = InputManager.inputInteger(menuTranslation.getTranslation("ageInput"), true);
            age = (age == null ? 0 : age); // age defaults to 0
        }
        do {
            userInput = InputManager.inputChar(menuTranslation.getTranslation("favoriteCategoriesChange"), true);
            if (userInput != null && userInput != 'S' && userInput != 'N')
                userInput = null;
        } while (userInput == null);
        if (userInput == 'S') {
            favoriteCategories = selectFavoriteCategories();
        }
        return new User(currentUser.getUsername(), currentUser.getHashedPassword(), currentUser.getUserID(), currentUser.getGender(), age, favoriteCategories);
    }

    /**
     * Prints all user's notifications and allows to mark them as read
     * @param user Current User object to fetch notifications
     */
    public void displayNotifications (User user) {
        ArrayList<Notification> notifications;
        try {
            notifications = dbConnection.getAllNotificationsByUser(user);
        } catch (SQLException e) {
            System.err.println();
            return;
        }
        if (notifications.size() == 0) {
            System.out.println(menuTranslation.getTranslation("noPersonalNotifications"));
            return;
        }

        StringBuilder sb = new StringBuilder(); // Lots of eye candy from here on
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

        for (int i = 0; i < notifications.size(); i++) {
            Notification notification = notifications.get(i);
            String notificationStatus = notification.isRead() ? notificationRead : notificationUnread;
            String statusSpacer = notification.isRead() ? notificationReadSpacer : notificationUnreadSpacer;

            sb.append(i + 1).append(") ");
            sb.append(notificationStatus);
            sb.append(statusSpacer);
            sb.append(" - ");
            sb.append(notification.getTitle()).append('\n');
            sb.append(notification.getContent()).append('\n');
        }
        System.out.println(sb);

        Integer userSelection;
        while (true) {
            userSelection = InputManager.inputInteger(menuTranslation.getTranslation("selectNotificationToSetAsRead"), false);
            if (userSelection == null || userSelection <= 0 || userSelection > notifications.size()) {
                System.out.println(menuTranslation.getTranslation("invalidUserSelection"));
                return;
            }
            Notification notification = notifications.get(userSelection - 1);
            notification.setRead(true);
            try {
                dbConnection.updateNotificationRead(notification);
            } catch (SQLException e) {
                System.err.println(menuTranslation.getTranslation("errorSettingNotificationAsRead"));
            }
        }
    }

    /**
     * Display all the events an user has created
     * @param user Current User object to fetch events
     */
    public Event displayAndSelectCreatedEvents(User user) {
        Path eventJsonPath = Paths.get(JsonTranslator.EVENT_JSON_PATH);
        JsonTranslator eventTranslation = new JsonTranslator(eventJsonPath.toString());
        ArrayList<Event> eventsInDB = null;
        try {
            eventsInDB = dbConnection.getEventsByCreator(user);
            for (int i = 0; i < eventsInDB.size(); i++) {
                Event event = eventsInDB.get(i);
                System.out.println((i + 1) + ") " + event.synopsis(eventTranslation, dbConnection));
            }
        } catch (SQLException e) {
            System.err.println(menuTranslation.getTranslation("SQLError"));
            e.printStackTrace();
            System.exit(1);
        } catch (NoSuchElementException ex) {
            System.out.println(menuTranslation.getTranslation("noCreatedEvents"));
            return null;
        }

        Integer userSelection = InputManager.inputInteger(menuTranslation.getTranslation("selectEventToShow"), false);
        if (userSelection == null || userSelection <= 0 || userSelection > eventsInDB.size()) {
            System.out.println(menuTranslation.getTranslation("invalidUserSelection"));
            return null;
        }
        return eventsInDB.get(userSelection - 1);
    }

    /**
     * Display all the events an user has registered to
     * @param user Current User object to fetch events
     * @param currentDateTime
     */
    public void displayEventsByRegistration(User user, LocalDateTime currentDateTime) {
        Path eventJsonPath = Paths.get(JsonTranslator.EVENT_JSON_PATH);
        JsonTranslator eventTranslation = new JsonTranslator(eventJsonPath.toString());

        ArrayList<Event> eventsInDB = null;

        try {
            eventsInDB = dbConnection.getEventsByRegistration(user);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < eventsInDB.size(); i++) {
                sb.append(i + 1).append(") ");
                sb.append(eventsInDB.get(i).synopsis(eventTranslation, dbConnection));
            }
            System.out.println(sb);
        } catch (SQLException e) {
            System.err.println(menuTranslation.getTranslation("SQLError"));
            e.printStackTrace();
            System.exit(1);
        } catch (NoSuchElementException ex) {
            System.out.println(menuTranslation.getTranslation("noRegisteredEvents"));
            return;
        }

        Integer userSelection;
        while (true) {
            userSelection = InputManager.inputInteger(menuTranslation.getTranslation("selectEventToDeregister"), false);
            if (userSelection == null || userSelection <= 0 || userSelection > eventsInDB.size()) {
                System.out.println(menuTranslation.getTranslation("invalidUserSelection"));
                return;
            }
            Event event = eventsInDB.get(userSelection - 1);
            boolean deregister = false;

            try {
                deregister = event.deregister(user, currentDateTime);
            } catch (IllegalStateException | IllegalArgumentException e) {
                System.err.println(menuTranslation.getTranslation("errorEventDeregistration"));
                System.err.println(e.getMessage());
            }
            if (deregister) {
                try {
                    dbConnection.updateEventRegistrations(event);
                } catch (SQLException e) {
                    System.err.println(menuTranslation.getTranslation("errorDeregisteringEvent"));
                    System.err.println(e.getMessage());
                }
                if (event.updateState(currentDateTime)) {
                    // A user has deregistered while the event was CLOSED, so now it's OPEN again
                    try {
                        dbConnection.updateEventState(event);
                    } catch (SQLException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
            }
        }
    }

    /**
     * Prompt the user a list of available categories.
     * Once the user has selected the one he wants, proceeds to create an object of the right kind and fill its fields.
     * If the user chooses 0 or a number bigger than the number of available categories, null is returned.
     * @param user Current user who will be the creator of the Event
     * @param currentDateTime
     * @return Event object of the right sub-class with required fields compiled - WARNING: can be null!
     *
     */
    public Event createEvent(User user, LocalDateTime currentDateTime) throws IllegalStateException {
        EventFactory eFactory = new EventFactory();
        Event event = null;
        StringBuilder sb = new StringBuilder();

        sb.append(menuTranslation.getTranslation("categoryList")).append('\n');
        ArrayList<String> categories = dbConnection.getCategories();
        for (int i = 0; i < categories.size(); i++) {
            ArrayList<String> catDescription = getCategoryDescription(categories.get(i));
            // Numbers printed below will be 1 based, so to select the right category user input hast to be decremented by one
            sb.append(i + 1).append(") ").append(catDescription.get(0)).append("\n     ").append(catDescription.get(1)).append('\n');
        }
        System.out.println(sb);

        Integer userSelection = InputManager.inputInteger(menuTranslation.getTranslation("userSelection"), false);
        if (userSelection == null || userSelection <= 0 || userSelection > categories.size()) {
            System.out.println(menuTranslation.getTranslation("invalidUserSelection"));
            return null;
        }

        event = eFactory.createEvent(UUID.randomUUID(), user.getUserID(), categories.get(userSelection - 1));

        fillEventFields(event, currentDateTime);

        return event;
    }

    /**
     * Asks the user to choose if he wants to register to a Event.
     * Useful only for user interaction, no information about the event itself is actually needed for this method.
     * @return true if the user wants to register, false otherwise
     *
     */
    public boolean registerEvent() {
        Character userInput = null;

        do {
            userInput = InputManager.inputChar(menuTranslation.getTranslation("eventRegistration"), true);
            if (userInput != null && userInput != 'S' && userInput != 'N')
                userInput = null;
        } while (userInput == null);
        return userInput == 'S';
    }

    /**
     * Asks the user to choose which OptionalCosts he's interested into
     * @param allCostsMap a LinkedHashMap with a String as a key and a OptionalCost as Value
     *                      - Key is costs's name as a String (such as the one returned from getAttributesName)
     *                        i.e. "lodge" for MountainHiking
     *                      - Value is a OptionalField object
     * @return a LinkedHashMap such as allCostsMap, but with only the wanted optional costs, so it's a subset of
     *         the original maps
     */
    public LinkedHashMap<String, OptionalCost> wantedOptionalCosts(LinkedHashMap<String, OptionalCost> allCostsMap) {
        if (allCostsMap == null) return null;
        Character userInput = null;
        Iterator iterator  = allCostsMap.entrySet().iterator(); // Get an iterator for our map
        LinkedHashMap<String, OptionalCost> wantedCosts = new LinkedHashMap<>();

        Path eventJsonPath = Paths.get(JsonTranslator.EVENT_JSON_PATH);
        JsonTranslator eventTranslation = new JsonTranslator(eventJsonPath.toString());

        while(iterator.hasNext()) {
            Map.Entry entry = (Map.Entry)iterator.next(); // Casts the iterated item to a Map Entry to use it as such
            String fieldTranslation = eventTranslation.getName((String) entry.getKey());
            do {
                userInput = InputManager.inputChar(String.format(menuTranslation.getTranslation("optionalCostRegistration"), fieldTranslation), true);
                if (userInput != null && userInput != 'S' && userInput != 'N')
                    userInput = null;
            } while (userInput == null);
            if (userInput == 'S') {
                // The user wants this optional cost
                wantedCosts.put((String)entry.getKey(), (OptionalCost)entry.getValue());
            }
        }
        if (wantedCosts.size() == 0)
            return null;
        return wantedCosts;
    }

    /**
     * Asks the user to choose if he wants to send invites for a Event.
     * Useful only for user interaction, no information about the event itself is actually needed for this method.
     * @return true if the user wants to send invites
     *
     */
    public boolean sendInvite() {
        Character userInput = null;

        do {
            userInput = InputManager.inputChar(menuTranslation.getTranslation("sendInvite"), true);
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
    public boolean publishEvent() {
        Character userInput = null;

        do {
            userInput = InputManager.inputChar(menuTranslation.getTranslation("eventPublication"), true);
            if (userInput != null && userInput != 'S' && userInput != 'N')
                userInput = null;
        } while (userInput == null);
        return userInput == 'S';
    }

    /**
     * Asks the user to choose if he wants the Event to be withdrawn from the Event List.
     * Useful only for user interaction, no information about the event itself is actually needed for this method.
     * @return true if the event has to be withdrawn, false otherwise
     *
     */
    public boolean withdrawEvent() {
        Character userInput = null;

        do {
            userInput = InputManager.inputChar(menuTranslation.getTranslation("eventWithdraw"), true);
            if (userInput != null && userInput != 'S' && userInput != 'N')
                userInput = null;
        } while (userInput == null);
        return userInput == 'S';
    }

    /**
     * Asks the user to choose which category of events he's interested to among available ones.
     * WARNING Can return null object if the user did not select any category
     * @return An ArrayList of String objects containing categories names as saved in database table "categories"
     */
    private String[] selectFavoriteCategories () {
        ArrayList<String> availableCategories = dbConnection.getCategories();
        StringBuilder sb = new StringBuilder();
        int max = 0;

        sb.append(menuTranslation.getTranslation("categoryList")).append('\n');
        for (int i = 0; i < availableCategories.size(); i++) {
            sb.append(i + 1).append(") ");
            sb.append(this.getCategoryDescription(availableCategories.get(i)).get(0)).append('\n');
        }
        System.out.println(sb);

        ArrayList<Integer> userNumbers = InputManager.inputNumberSequence(menuTranslation.getTranslation("favoriteCategoriesInput"), true);
        ArrayList<String> selectedCategories = new ArrayList<>();
        if (userNumbers == null) {
            System.out.println(menuTranslation.getTranslation("noFavoriteCategorySelected"));
            return null;
        }
        for (Integer number : userNumbers)
            if (number - 1 >= availableCategories.size() || number <= 0) { // Categories are printed starting from number 1
                System.err.println(menuTranslation.getTranslation("invalidFavoriteCategorySelected"));
                return null; // out of bound
            } else {
                selectedCategories.add(availableCategories.get(number - 1));
            }

        return selectedCategories.toArray(new String[selectedCategories.size()]);
    }

    /**
     * Fills all the fields of a given it.unibs.ing.se.model.Event object
     * @throws IllegalStateException if user input is logically inconsistent (start date after end date and so on)
     */
    private void fillEventFields(Event event, LocalDateTime currentDateTime) throws IllegalStateException {
        Path eventJsonPath = Paths.get(JsonTranslator.EVENT_JSON_PATH);
        JsonTranslator eventTranslation = new JsonTranslator(eventJsonPath.toString());

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

        event.setAttribute("participantsMax", event.participantsMin + (event.participantsSurplus == null ? 0 : event.participantsSurplus));

        if (event.deregistrationDeadline == null) // As specified by client request, if deregistration deadline wasn't set...
            event.setAttribute("deregistrationDeadline", event.registrationDeadline); // it defaults to registration deadline

        event.isLegal(currentDateTime);
    }

    /**
     * Prints name and description of available categories' fields
     */
    private void printEventFieldsName(Event event) {
        Path eventJsonPath = Paths.get(JsonTranslator.EVENT_JSON_PATH);
        JsonTranslator eventTranslation = new JsonTranslator(eventJsonPath.toString());

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
     * // TODO remove this method altogether
     */
    public static ArrayList<String> getCategoryDescription(String eventType) {
        Path eventJsonPath = Paths.get(JsonTranslator.EVENT_JSON_PATH);
        JsonTranslator eventTranslation = new JsonTranslator(eventJsonPath.toString());

        ArrayList<String> returnCatDescr = new ArrayList<>();

        returnCatDescr.add(0, eventTranslation.getName(eventType));
        returnCatDescr.add(1, eventTranslation.getDescr(eventType));

        return returnCatDescr;
    }
}
