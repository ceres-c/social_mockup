package impl;

import impl.fields.Sex;

import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

class Connector {
    private final static String GET_USER = "SELECT * FROM public.users WHERE username = ?";
    private final static String INSERT_USER = "INSERT INTO public.users values (?, ?, ?, ?)";
    private final static String GET_USERNAME = "SELECT username FROM public.users WHERE userID = ?";
    private final static String GET_CATEGORIES_LIST = "select * from public.categories";
    private final static String GET_EVENT_TYPE = "SELECT eventType FROM public.default_event WHERE eventID = ?";
    private final static String GET_ACTIVE_EVENTS_LIST = "SELECT eventID FROM public.default_event WHERE published = true AND currentstate <> 'ENDED' AND currentstate <> 'FAILED'";
    private final static String GET_OPEN_EVENTS_LIST = "SELECT eventID FROM public.default_event WHERE published = true AND currentstate = 'OPEN'";
    private final static String GET_EVENTS_LIST_BY_CREATORID = "SELECT eventID FROM public.default_event WHERE creatorID = ?";
    private final static String GET_EVENTS_LIST_BY_REGISTERED = "SELECT eventID FROM public.default_event WHERE ? = ANY (public.default_event.registeredUsers)";
    private final static String GET_EVENT = "SELECT * FROM public.%s WHERE eventID LIKE ?"; // HACK explained in function body
    private final static String UPDATE_EVENT_STATE = "UPDATE public.default_event SET currentstate = ? WHERE eventID = ?";
    private final static String UPDATE_EVENT_PUBLISHED = "UPDATE public.default_event SET published = ? WHERE eventID = ?";
    private final static String UPDATE_EVENT_REGISTERED = "UPDATE public.default_event SET registeredUsers = ? WHERE eventID = ?";
    private final static String GET_NOTIFICATIONS_BY_USER = "SELECT notificationID FROM public.eventNotifications WHERE recipientID = ?";
    private final static String GET_UNREAD_NOTIFICATIONS_BY_USER = "SELECT * FROM public.eventNotifications WHERE recipientID = ? AND read = false";
    private final static String GET_NOTIFICATION = "SELECT * FROM public.eventNotifications WHERE notificationID = ?";
    private final static String UPDATE_NOTIFICATION_READ = "UPDATE public.eventNotifications SET read = ? WHERE notificationID = ?";
    private final static String INSERT_NOTIFICATION = "INSERT INTO public.eventnotifications values (?, ?, ?, ?, ?, ?)";

    private Connection dbConnection = null;

    /**
     *
     * @param dbURL Url to target DB - should start with "jdbc:postgresql://"
     * @param username String with database username
     * @param password String with database password
     */
    Connector(String dbURL, String username, String password) throws SQLException {
        try {
            Class.forName("org.postgresql.Driver"); // Ensures correct driver is loaded
        } catch (java.lang.ClassNotFoundException e) {
            System.out.println("ALERT: Error selecting postgresql driver!");
            e.printStackTrace();
        }
        dbConnection = DriverManager.getConnection(dbURL, username, password);
    }

    /**
     * Closes a connection to the database
     * @throws IllegalStateException If no database is currently open
     */
    void closeDb () throws IllegalStateException {
        if (dbConnection == null) throw new IllegalStateException("ALERT: No connection to the database");
        try {
            dbConnection.close();
        } catch (java.sql.SQLException e) {
            System.out.println("ALERT: Error closing database connection!");
            e.printStackTrace();
        }
        dbConnection = null;
    }

    /**
     * Validates given username and password on currently existing users and, if correct, returns a User object with
     * data currently saved in the database
     * @param username String with user's username
     * @param hashedPassword String with user's hashed password
     * @return User object with data from the database
     * @throws IllegalStateException If called before a database connection is established
     * @throws IllegalArgumentException If username or password are wrong
     * @throws SQLException Directly from SQL driver if something else bad happens
     */
    User getUser(String username, String hashedPassword) throws IllegalStateException, IllegalArgumentException, SQLException {
        if (dbConnection == null) throw new IllegalStateException("ALERT: No connection to the database");
        String query = GET_USER;

        User returnUser = new User();
        PreparedStatement userExistStatement = dbConnection.prepareStatement(query);
        userExistStatement.setString(1, username);
        ResultSet rs = userExistStatement.executeQuery();

        if (!rs.next()) { // rs.next() returns false when the query has no results (username not present)
            throw new IllegalArgumentException("Wrong username or password"); // To avoid leaking information about existing users
        } else if (!hashedPassword.equals(rs.getString(2))) {
            throw new IllegalArgumentException("Wrong username or password"); // To avoid leaking information about existing users
        } else {
            returnUser.setUsername(rs.getString("username"));
            returnUser.setHashedPassword(rs.getString("hashedPassword"));
            returnUser.setUserID(UUID.fromString(rs.getString("userID")));
            returnUser.setGender(new Sex(rs.getString("gender")));
        }
        return returnUser;
    }

    /**
     * Insert a User object into the database
     * @param user User object already populated
     * @throws IllegalStateException If called before a database connection is established
     * @throws IllegalArgumentException If a User with the same username already exists in the database
     * @throws SQLException Directly from SQL driver if something else bad happens
     * @return True if insertion went right
     */
    boolean insertUser (User user) throws IllegalStateException, IllegalArgumentException, SQLException {
        if (dbConnection == null) throw new IllegalStateException("ALERT: No connection to the database");

        int i = 0;
        try {
            PreparedStatement insertUserStatement = dbConnection.prepareStatement(INSERT_USER);
            insertUserStatement.setString(1, user.getUsername());
            insertUserStatement.setString(2, user.getHashedPassword());
            insertUserStatement.setString(3, user.getUserIDAsString());
            insertUserStatement.setString(4, user.getGender().toString());
            i = insertUserStatement.executeUpdate();
        } catch(java.sql.SQLException e) {
            if (e.getMessage().contains("duplicate key value")) {
                throw new IllegalArgumentException("ALERT: Duplicate user " + user.getUsername());
            }
            System.out.println("ALERT: Failed to insert user in database");
            e.printStackTrace();
        }
        if (i == 1)
            return true;
        else
            throw new SQLException("ALERT: Error adding user to the database!\nSQL INSERT query returned " + i);
    }

    /**
     * Returns only username of a user given its userID.
     * @param userID UUID object to look for in the database
     * @return String with the username
     * @throws IllegalStateException If called before a database connection is established
     * @throws IllegalArgumentException If username or password are wrong
     * @throws SQLException Directly from SQL driver if something else bad happens
     */
    String getUsername(UUID userID) throws IllegalStateException, IllegalArgumentException, SQLException {
        if (dbConnection == null) throw new IllegalStateException("ALERT: No connection to the database");

        String username;
        PreparedStatement userExistStatement = dbConnection.prepareStatement(GET_USERNAME);
        userExistStatement.setString(1, userID.toString());
        ResultSet rs = userExistStatement.executeQuery();

        if (!rs.next()) { // rs.next() returns false when the query has no results (userID not present)
            throw new IllegalArgumentException("ALERT: Specified user does not exist");
        } else {
            username = rs.getString("username");
        }
        return username;
    }

    /**
     * Fetches categories currently present in the database
     * @return  an ArrayList of String in the first column of public.categories table.
     *          If no categories, empty ArrayList.
     * @throws IllegalStateException If called before a database connection is established
     * @throws NoSuchElementException If the database table public.categories is empty
     */
    ArrayList<String> getCategories() throws IllegalStateException, NoSuchElementException {
        if (dbConnection == null) throw new IllegalStateException("ALERT: No connection to the database");

        ArrayList<String> returnCategories = new ArrayList<>();
        try {
            Statement categoriesStatement = dbConnection.createStatement();
            ResultSet rs = categoriesStatement.executeQuery(GET_CATEGORIES_LIST);

            if (!rs.next()) { // rs.next() returns false when the query has no results
                throw new NoSuchElementException("ALERT: No categories in the database");
            } else {
                do {
                    returnCategories.add(rs.getString(1));
                } while (rs.next());
            }
        } catch(java.sql.SQLException e) {
            System.out.println("ALERT: Failed getting categories from database!");
            e.printStackTrace();
        }
        return returnCategories;
    }

    /**
     * Inserts an Event into the database in the proper table given an Event-like object
     * @return  true if everything went smoothly
     * @throws IllegalStateException If called before a database connection is established
     * @throws SQLException If a database access error occurs
     * @throws SQLTimeoutException When the driver has determined that the timeout value that was specified
     *                             by the setQueryTimeout method has been exceeded and has at least
     *                             attempted to cancel the currently running Statement
     */
    boolean insertEvent (Event event) throws IllegalStateException, SQLException, SQLTimeoutException {
        if (dbConnection == null) throw new IllegalStateException("ALERT: No connection to the database");

        int parametersNumber = 0;
        StringBuilder query = new StringBuilder();
        query.append("INSERT INTO public."); // Beginning of query
        query.append(event.getEventTypeDB()).append(" ("); // category name. Eg: "INSERT INTO public.soccer_game ("
        // Event private fields can't be retrieved via getNonNullAttributesWithValue, also, these are common to all categories
        query.append("eventID, creatorID, eventType, published, registeredUsers, currentState, participantsMax");
        parametersNumber += 7; // Number of Event private fields

        Iterator iterator;
        LinkedHashMap<String, Object> setAttributes = event.getNonNullAttributesWithValue(); // Map with all currently valid attributes
        iterator = setAttributes.entrySet().iterator(); // Get an iterator for our map

        while(iterator.hasNext()) {
            Map.Entry entry = (Map.Entry)iterator.next(); // Casts the iterated item to a Map Entry to use it as such
            query.append(", ").append(entry.getKey()); // adds the field (column) name to the query. Eg: ", title"
            parametersNumber++;
        }
        query.append(") VALUES (?"); // Terminates fields (columns) list and starts placeholders. Notice the first question mark!
        for (int i = 0; i < parametersNumber - 1; i++) // Minus one since the first question mark is present in the above line
            query.append(", ?");
        query.append(")"); // We now have the query that can be passed to PreparedStatement


        int parameterIndex = 1; // Because starting at 1 is fun, Java devs thought
        PreparedStatement addEventStatement = dbConnection.prepareStatement(query.toString());
        addEventStatement.setString(parameterIndex, event.getEventIDAsString()); // Event private fields here - eventID
        parameterIndex++;
        addEventStatement.setString(parameterIndex, event.getCreatorIDAsString()); // creatorID
        parameterIndex++;
        addEventStatement.setString(parameterIndex, event.getEventTypeDB()); // eventType
        parameterIndex++;
        addEventStatement.setBoolean(parameterIndex, event.isPublished()); // published
        parameterIndex++;
        Array registeredUUIDDBArray = dbConnection.createArrayOf("VARCHAR", event.getRegisteredUsersAsString().toArray()); // registeredUsers
        addEventStatement.setArray(parameterIndex, registeredUUIDDBArray);
        parameterIndex++;
        addEventStatement.setString(parameterIndex, event.getCurrentStateAsString()); // currentState
        parameterIndex++;
        addEventStatement.setInt(parameterIndex, event.getParticipantsMax()); // participantsMax
        parameterIndex++;

        Class type; // This will hold the object type
        iterator = setAttributes.entrySet().iterator(); // Reset to first element
        while(iterator.hasNext()) {
            Map.Entry entry = (Map.Entry)iterator.next(); // Casts the iterated item to a Map Entry to use it as such
            type = entry.getValue().getClass();
            if (type.equals(Integer.class)) {
                addEventStatement.setInt(parameterIndex, (int) entry.getValue());
            } else if (type.equals(Double.class)) {
                addEventStatement.setDouble(parameterIndex, (double) entry.getValue());
            } else if (type.equals(String.class)) {
                addEventStatement.setString(parameterIndex, entry.getValue().toString());
            } else if (type.equals(LocalDateTime.class)) {
                addEventStatement.setObject(parameterIndex, (LocalDateTime) entry.getValue()); // Postgresql driver natively supports LocalDateTime
            } else if (type.equals(Duration.class)) {
                addEventStatement.setLong(parameterIndex, ((Duration) entry.getValue()).getSeconds());
            } else if (type.equals(Sex.class)) {
                addEventStatement.setString(parameterIndex, ((Sex) entry.getValue()).toString());
            } else {
                throw new IllegalArgumentException("ALERT: Unexpected input type: " + type);
            }
            parameterIndex++;
        }

        int i = addEventStatement.executeUpdate();
        return i == 1; // executeUpdate returns 1 if the row has been added successfully
    }

    /**
     * Updates an Event state
     * @param event The Event to update in database
     * @return True if everything went smoothly
     * @throws IllegalStateException If called before a database connection is established
     * @throws SQLException If a database access error occurs
     * @throws SQLTimeoutException When the driver has determined that the timeout value that was specified
     *                             by the setQueryTimeout method has been exceeded and has at least
     *                             attempted to cancel the currently running Statement
     */
    boolean updateEventState(Event event) throws IllegalStateException, SQLException, SQLTimeoutException {
        if (dbConnection == null) throw new IllegalStateException("ALERT: No connection to the database");

        int i = 0;
        PreparedStatement updateEventPublishedStatement = dbConnection.prepareStatement(UPDATE_EVENT_STATE);
        updateEventPublishedStatement.setString(1, event.getCurrentStateAsString()); // published
        updateEventPublishedStatement.setString(2, event.getEventIDAsString()); // eventID
        i = updateEventPublishedStatement.executeUpdate();
        if (i != 1)
            throw new SQLException("ALERT: Error adding updating event!\nSQL INSERT query returned " + i);
        return true;
    }

    /**
     * Updates an Event publishing status
     * @param event The Event to update in database
     * @return True if everything went smoothly
     * @throws IllegalStateException If called before a database connection is established
     * @throws SQLException If a database access error occurs
     * @throws SQLTimeoutException When the driver has determined that the timeout value that was specified
     *                             by the setQueryTimeout method has been exceeded and has at least
     *                             attempted to cancel the currently running Statement
     */
    boolean updateEventPublished(Event event) throws IllegalStateException, SQLException, SQLTimeoutException {
        if (dbConnection == null) throw new IllegalStateException("ALERT: No connection to the database");

        int i = 0;
        PreparedStatement updateEventPublishedStatement = dbConnection.prepareStatement(UPDATE_EVENT_PUBLISHED);
        updateEventPublishedStatement.setBoolean(1, event.isPublished()); // published
        updateEventPublishedStatement.setString(2, event.getEventIDAsString()); // eventID
        i = updateEventPublishedStatement.executeUpdate();
        if (i != 1)
            throw new SQLException("ALERT: Error adding updating event!\nSQL INSERT query returned " + i);
        return true;
    }

    /**
     * Updates an Event registered users
     * @param event The Event to update in database
     * @return True if everything went smoothly
     * @throws IllegalStateException If called before a database connection is established
     * @throws SQLException If a database access error occurs
     * @throws SQLTimeoutException When the driver has determined that the timeout value that was specified
     *                             by the setQueryTimeout method has been exceeded and has at least
     *                             attempted to cancel the currently running Statement
     */
    boolean updateEventRegistrations(Event event) throws IllegalStateException, SQLException, SQLTimeoutException {
        if (dbConnection == null) throw new IllegalStateException("ALERT: No connection to the database");

        int i = 0;
        PreparedStatement updateEventPublishedStatement = dbConnection.prepareStatement(UPDATE_EVENT_REGISTERED);
        Array registeredUUIDDBArray = dbConnection.createArrayOf("VARCHAR", event.getRegisteredUsersAsString().toArray());
        updateEventPublishedStatement.setArray(1, registeredUUIDDBArray); // registeredUsers
        updateEventPublishedStatement.setString(2, event.getEventIDAsString()); // eventID
        i = updateEventPublishedStatement.executeUpdate();
        if (i != 1)
            throw new SQLException("ALERT: Error adding updating event!\nSQL INSERT query returned " + i);
        return true;
    }

    /**
     * Dumps all non-ENDED and non-FAILED Events in the database to an ArrayList
     * @return ArrayList of Event objects
     * @throws IllegalStateException If called before a database connection is established
     * @throws SQLException If a database access error occurs
     */
    ArrayList<Event> getActiveEvents() throws IllegalStateException, SQLException {
        if (dbConnection == null) throw new IllegalStateException("ALERT: No connection to the database");

        ArrayList<Event> returnEvents = new ArrayList<>();

        Statement getEventsStatement = dbConnection.createStatement();
        ResultSet rs = getEventsStatement.executeQuery(GET_ACTIVE_EVENTS_LIST);

        if (!rs.next()) { // rs.next() returns false when the query has no results
            throw new NoSuchElementException("ALERT: No events in the database");
        } else {
            do {
                returnEvents.add(getEvent(UUID.fromString(rs.getString(1))));
            } while (rs.next());
        }

        return returnEvents;
    }

    /**
     * Dumps all OPEN Events in the database to an ArrayList
     * @return ArrayList of Event objects
     * @throws IllegalStateException If called before a database connection is established
     * @throws SQLException If a database access error occurs
     */
    ArrayList<Event> getOpenEvents() throws IllegalStateException, SQLException {
        if (dbConnection == null) throw new IllegalStateException("ALERT: No connection to the database");

        ArrayList<Event> returnEvents = new ArrayList<>();

        Statement getEventsStatement = dbConnection.createStatement();
        ResultSet rs = getEventsStatement.executeQuery(GET_OPEN_EVENTS_LIST);

        if (!rs.next()) { // rs.next() returns false when the query has no results
            throw new NoSuchElementException("ALERT: No events in the database");
        } else {
            do {
                returnEvents.add(getEvent(UUID.fromString(rs.getString(1))));
            } while (rs.next());
        }

        return returnEvents;
    }

    /**
     * Gets all the Events a user has created and can administer
     * @param user User object for which we're looking for events
     * @return ArrayList of Event objects
     * @throws IllegalStateException If called before a database connection is established
     * @throws SQLException If a database access error occurs
     */
    ArrayList<Event> getEventsByCreator(User user) throws IllegalStateException, SQLException {
        if (dbConnection == null) throw new IllegalStateException("ALERT: No connection to the database");

        ArrayList<Event> returnEvents = new ArrayList<>();

        PreparedStatement getEventsByCreatorStatement = dbConnection.prepareStatement(GET_EVENTS_LIST_BY_CREATORID);
        getEventsByCreatorStatement.setString(1, user.getUserIDAsString());
        ResultSet rs = getEventsByCreatorStatement.executeQuery();

        if (!rs.next()) { // rs.next() returns false when the query has no results
            throw new NoSuchElementException("ALERT: No events were created by user " + user.getUserIDAsString());
        } else {
            do {
                returnEvents.add(getEvent(UUID.fromString(rs.getString(1))));
            } while (rs.next());
        }

        return returnEvents;
    }

    /**
     * Gets all the Event a user has registered to
     * @param user User object for which we're looking for events
     * @return ArrayList of Event objects
     * @throws IllegalStateException If called before a database connection is established
     * @throws SQLException If a database access error occurs
     */
    ArrayList<Event> getEventsByRegistration(User user) throws IllegalStateException, SQLException {
        if (dbConnection == null) throw new IllegalStateException("ALERT: No connection to the database");

        ArrayList<Event> returnEvents = new ArrayList<>();

        PreparedStatement getEventsByCreatorStatement = dbConnection.prepareStatement(GET_EVENTS_LIST_BY_REGISTERED);
        getEventsByCreatorStatement.setString(1, user.getUserIDAsString());
        ResultSet rs = getEventsByCreatorStatement.executeQuery();

        if (!rs.next()) { // rs.next() returns false when the query has no results
            throw new NoSuchElementException("ALERT: User " + user.getUserIDAsString() + " hasn't registered for any event");
        } else {
            do {
                returnEvents.add(getEvent(UUID.fromString(rs.getString(1))));
            } while (rs.next());
        }

        return returnEvents;
    }

    /**
     * Insert a Notification object into the database
     * @param notification Notification object already populated
     * @throws IllegalStateException If called before a database connection is established
     * @throws SQLException Directly from SQL driver if something else bad happens
     * @return True if insertion went right
     */
    boolean insertNotification(Notification notification) throws IllegalStateException, SQLException {
        if (dbConnection == null) throw new IllegalStateException("ALERT: No connection to the database");

        int i = 0;
            PreparedStatement insertUserStatement = dbConnection.prepareStatement(INSERT_NOTIFICATION);
            insertUserStatement.setString(1, notification.getNotificationID().toString());
            insertUserStatement.setString(2, notification.getEventID().toString());
            insertUserStatement.setString(3, notification.getRecipientID().toString());
            insertUserStatement.setBoolean(4, notification.isRead());
            insertUserStatement.setString(5, notification.getTitle());
            insertUserStatement.setString(6, notification.getContent());
            i = insertUserStatement.executeUpdate();
        if (i == 1)
            return true;
        else
            throw new SQLException("ALERT: Error adding user to the database!\nSQL INSERT query returned " + i);
    }

    /**
     * Gets the list of all the Notifications relevant to a specified User
     * @param user The User object to search notifications for
     * @return ArrayList of Notification objects - can be 0 elements long if there are notifications
     * @throws IllegalStateException If called before a database connection is established
     * @throws SQLException If a database access error occurs
     */
    ArrayList<Notification> getAllNotificationsByUser(User user) throws IllegalStateException, SQLException {
        if (dbConnection == null) throw new IllegalStateException("ALERT: No connection to the database");

        ArrayList<Notification> returnNotifications = new ArrayList<>();

        PreparedStatement getNotificationsByUserStatement = dbConnection.prepareStatement(GET_NOTIFICATIONS_BY_USER);
        getNotificationsByUserStatement.setString(1, user.getUserIDAsString());
        ResultSet rs = getNotificationsByUserStatement.executeQuery();

        if (!rs.next()) { // rs.next() returns false when the query has no results
            return returnNotifications;
        } else {
            do {
                UUID notificationID = UUID.fromString(rs.getString(1));
                returnNotifications.add(getNotification(notificationID));
            } while (rs.next());
        }

        return returnNotifications;
    }

    /**
     * Returns the number of unread notifications a user have
     * @param user User object for which we're looking for notifications
     * @return Number of unread notifications
     * @throws IllegalStateException If called before a database connection is established
     * @throws SQLException If a database access error occurs
     */
    int getUnreadNotificationsCountByUser(User user) throws IllegalStateException, SQLException {
        if (dbConnection == null) throw new IllegalStateException("ALERT: No connection to the database");

        int count = 0;

        PreparedStatement getNotificationsByUserStatement = dbConnection.prepareStatement(GET_UNREAD_NOTIFICATIONS_BY_USER);
        getNotificationsByUserStatement.setString(1, user.getUserIDAsString());
        ResultSet rs = getNotificationsByUserStatement.executeQuery();

        while (rs.next()) count++;

        return count;
    }

    boolean updateNotificationRead(Notification notification) throws IllegalStateException, SQLException {
        if (dbConnection == null) throw new IllegalStateException("ALERT: No connection to the database");

        int i = 0;
        PreparedStatement updateEventPublishedStatement = dbConnection.prepareStatement(UPDATE_NOTIFICATION_READ);
        updateEventPublishedStatement.setBoolean(1, notification.isRead()); // read status
        updateEventPublishedStatement.setString(2, notification.getNotificationID().toString()); // notificationID
        i = updateEventPublishedStatement.executeUpdate();
        if (i != 1)
            throw new SQLException("ALERT: Error adding updating event!\nSQL INSERT query returned " + i);
        return true;
    }

    /**
     * Returns a Notification object taken from the database given its UUID
     * @param notificationID UUID object with the UUID of the needed notification
     * @return Notification - An object with all its fields filled as they are in the database
     * @throws IllegalStateException If called before a database connection is established
     * @throws SQLException If a database access error occurs
     */
    private Notification getNotification(UUID notificationID) throws IllegalStateException, SQLException {
        if (dbConnection == null) throw new IllegalStateException("ALERT: No connection to the database");

        Notification returnNotification;

        PreparedStatement userExistStatement = dbConnection.prepareStatement(GET_NOTIFICATION);
        userExistStatement.setString(1, notificationID.toString());
        ResultSet rs = userExistStatement.executeQuery();

        if (!rs.next()) { // rs.next() returns false when the query has no results (notificationID not present)
            throw new IllegalArgumentException("ALERT: Specified notification does not exist");
        } else {
            UUID eventID = UUID.fromString(rs.getString("eventID"));
            UUID recipientID = UUID.fromString(rs.getString("recipientID"));
            boolean read = rs.getBoolean("read");
            String title = rs.getString("title");
            String content = rs.getString("content");
            returnNotification = new Notification(notificationID, eventID, recipientID, read, title, content);
        }
        return returnNotification;
    }

    /**
     * Returns an Event object taken from the database given its UUID
     * @param eventID UUID object with the ID of the required event
     * @return Event - An object with all its fields filled as they are in the database
     * @throws IllegalStateException If called before a database connection is established
     * @throws NoSuchElementException If the given UUID is not associated to any event in the database
     * @throws SQLException If a database access error occurs
     */
    private Event getEvent(UUID eventID) throws IllegalStateException, NoSuchElementException, SQLException {
        if (dbConnection == null) throw new IllegalStateException("ALERT: No connection to the database");

        Event event = null;
        EventFactory eFactory = new EventFactory();

        String eventType = this.getEventType(eventID);
        PreparedStatement eventStatement = dbConnection.prepareStatement(String.format(GET_EVENT, eventType)); // HACK!
        // This is a workaround to allow searching in the proper subtable (ie: soccer_game inherits from default_event)
        // Java PreparedStatements do not allow passing tables name, so this was the only solution that could save from inner joins
        eventStatement.setString(1, eventID.toString());
        ResultSet rs = eventStatement.executeQuery();

        if (!rs.next()) { // rs.next() returns false when the query has no results
            throw new NoSuchElementException("ALERT: No event with UUID " + eventID.toString() + " in database");
        } else {
            UUID creatorID = UUID.fromString(rs.getString("creatorID"));
            event = eFactory.createEvent(eventID, creatorID, eventType);

            event.setPublished(rs.getBoolean("published"));

            Array registeredUsersDbArray = rs.getArray("registeredUsers"); // Get a Sql.Array object from the database
            String[] registeredUsers = (String[])registeredUsersDbArray.getArray(); // Cast it to a Strings Array
            for (String userID: registeredUsers) {
                event.register(UUID.fromString(userID)); // Get UUID from String and add it to Event's array
            }

            event.setCurrentState(rs.getString("currentState"));

            event.setParticipantsMax(rs.getInt("participantsMax"));

            LinkedHashMap<String, Class<?>> eventFieldsMap = event.getAttributesWithType();

            Iterator iterator = eventFieldsMap.entrySet().iterator(); // Get an iterator for our map

            while(iterator.hasNext()) {
                Map.Entry entry = (Map.Entry)iterator.next(); // Casts the iterated item to a Map Entry to use it as such

                Object attributeValue = Connector.genericDBGetter(rs, (String)entry.getKey(), (Class)entry.getValue() );
                if (attributeValue != null) {
                    event.setAttribute((String) entry.getKey(), attributeValue);
                }
            }
        }

        return event;
    }

    /**
     * Returns an Event's type given its UUID
     * @param eventID UUID object with the UUID of the needed event
     * @return String - Event's category as present in DB category list: ie. "soccer_game"
     * @throws IllegalStateException If called before a database connection is established
     * @throws NoSuchElementException If the given UUID is not associated to any event in the database
     * @throws SQLException If a database access error occurs
     */
    private String getEventType (UUID eventID) throws IllegalStateException, NoSuchElementException, SQLException {
        if (dbConnection == null) throw new IllegalStateException("ALERT: No connection to the database");

        String eventType;
        PreparedStatement eventStatement = dbConnection.prepareStatement(GET_EVENT_TYPE);
        eventStatement.setString(1, eventID.toString());
        ResultSet rs = eventStatement.executeQuery();

        if (!rs.next()) { // rs.next() returns false when the query has no results
            throw new NoSuchElementException("ALERT: No event with UUID " + eventID.toString() + " in database");
        } else {
            eventType = rs.getString(1);
        }
        return eventType;
    }

    /**
     * Given a ResultSet object, column name and return type, this method chooses the right kind of database getter
     * WARNING: return is null if the cell is NULL
     * @param rs - A query ResultSet
     * @param columnName - The name of the column to which the object should be extracted from
     * @param type - The type of wanted object
     * @return A generic T which contains user input and HAS to be cast to the right type - WARNING: can be null!
     * @throws SQLException If a database access error occurs
     */
    private static <T> T genericDBGetter(ResultSet rs, String columnName, Class type) throws SQLException {
        if (type.equals(Integer.class)) {
            return (T) (Integer)rs.getInt(columnName);
        } else if (type.equals(Double.class)) {
            return (T) (Double)rs.getDouble(columnName);
        } else if (type.equals(String.class)) {
            return (T) rs.getString(columnName);
        } else if (type.equals(LocalDateTime.class)) {
            return (T) rs.getObject(columnName, LocalDateTime.class);
        } else if (type.equals(Duration.class)) {
            int seconds = rs.getInt(columnName);
            if (seconds == 0) return null; // Just for a matter of consistency
            return (T) Duration.of(seconds, ChronoUnit.SECONDS);
        } else if (type.equals(Sex.class)) {
            return (T) new Sex(rs.getString(columnName));
        } else {
            throw new IllegalArgumentException("ALERT: Unexpected input type: " + type);
        }
    }
}
