package impl;

import impl.fields.Sex;

import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

class Connector {
    private final static String SELECT_USER = "SELECT * FROM public.users WHERE username = ?";
    private final static String INSERT_USER = "INSERT INTO public.users values (?, ?, ?, ?)";
    private final static String GET_CATEGORIES_LIST = "select * from public.categories";
    private final static String GET_CATEGORY_DESCRIPTION = "SELECT * FROM public.categories WHERE event_type = ?";
    private final static String GET_EVENT_TYPE = "SELECT eventType FROM public.default_event WHERE eventID = ?";
    private final static String GET_EVENTS_LIST = "SELECT eventID FROM public.default_event";
    private final static String GET_EVENT = "SELECT * FROM public.%s WHERE eventID LIKE ?"; // HACK explained in function body
    private final static String UPDATE_EVENT_PUBLISHED = "UPDATE public.%s SET published = ? WHERE eventID = ?"; // HACK explained in function body
    private final static String UPDATE_EVENT_REGISTERED = "UPDATE public.%s SET registeredUsers = ? WHERE eventID = ?"; // HACK explained in function body

    Connection dbConnection = null;

    /**
     *
     * @param dbURL Url to target DB - should start with "jdbc:postgresql://"
     * @param username
     * @param password
     */
    Connector(String dbURL, String username, String password) {
        try {
            Class.forName("org.postgresql.Driver"); // Ensures correct driver is loaded
        } catch (java.lang.ClassNotFoundException e) {
            System.out.println("ALERT: Error selecting postgresql driver!");
            e.printStackTrace();
        }
        try {
            dbConnection = DriverManager.getConnection(dbURL, username, password);
        } catch(java.sql.SQLException e) {
            System.out.println("ALERT: Error establishing a database connection!");
            e.printStackTrace();
        }
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
     * @param username
     * @param hashedPassword
     * @return User object with data from the database
     * @throws IllegalStateException If called before a database connection is established
     * @throws IllegalArgumentException If username or password are wrong
     * @throws SQLException Directly from SQL driver if something else bad happens
     */
    User getUser(String username, String hashedPassword) throws IllegalStateException, IllegalArgumentException, SQLException {
        if (dbConnection == null) throw new IllegalStateException("ALERT: No connection to the database");
        String query = SELECT_USER;

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
     */
    void insertUser (User user) throws IllegalStateException, IllegalArgumentException, SQLException {
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
        if (i != 1)
            throw new SQLException("ALERT: Error adding user to the database!\nSQL INSERT query returned " + i);
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
     * Fetches a category's name and description from the database given its internal name (event_type in db)
     * @return  an ArrayList of String.
     *              - 1st element: Category's full name
     *              - 2nd element: Category's description
     *          If the category does not exist, empty ArrayList.
     * @throws IllegalStateException If called before a database connection is established
     * @throws NoSuchElementException If given category does not exist in public.categories table
     */
    ArrayList<String> getCategoryDescription(String categoryName) throws IllegalStateException, NoSuchElementException {
        if (dbConnection == null) throw new IllegalStateException("ALERT: No connection to the database");
        String query = GET_CATEGORY_DESCRIPTION;

        ArrayList<String> returnCatDescr = new ArrayList<>();
        try {
            PreparedStatement categoryDescrStatement = dbConnection.prepareStatement(query);
            categoryDescrStatement.setString(1, categoryName);
            ResultSet rs = categoryDescrStatement.executeQuery();

            if (!rs.next()) { // rs.next() returns false when the query has no results
                throw new NoSuchElementException("ALERT: Category " + categoryName + " does not exist in the database");
            } else {
                do {
                    returnCatDescr.add(rs.getString(2));
                    returnCatDescr.add(rs.getString(3));
                } while (rs.next());
            }
        } catch(java.sql.SQLException e) {
            System.out.println("ALERT: Failed getting category description!");
            e.printStackTrace();
        }
        return returnCatDescr;
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
        Iterator iterator;
        StringBuilder query = new StringBuilder();
        query.append("INSERT INTO public."); // Beginning of query
        query.append(event.getEventTypeDB()).append(" ("); // category name. Eg: "INSERT INTO public.soccer_game ("
        // Event private fields can't be retrieved via getNonNullAttributesWithValue, also, these names are common to all categories
        query.append("eventID, creatorID, eventType, published, registeredUsers, currentState");
        parametersNumber += 6; // Number of Event private fields

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
        addEventStatement.setString(parameterIndex, event.getCreatorID()); // creatorID
        parameterIndex++;
        addEventStatement.setString(parameterIndex, event.getEventTypeDB()); // eventType
        parameterIndex++;
        addEventStatement.setBoolean(parameterIndex, event.isPublished()); // published
        parameterIndex++;
        Array registeredUUIDDBArray = dbConnection.createArrayOf("VARCHAR", event.getRegisteredUsersAsString().toArray());
        addEventStatement.setArray(parameterIndex, registeredUUIDDBArray);
        parameterIndex++;
        addEventStatement.setString(parameterIndex, event.getState()); // currentState
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
     * Returns an Event object taken from the database given its UUID
     * @param eventID
     * @return Event - An object with all its fields filled as they are in the database
     * @throws IllegalStateException If called before a database connection is established
     * @throws NoSuchElementException If the given UUID is not associated to any event in the database
     * @throws SQLException If a database access error occurs
     */
    public Event getEvent(UUID eventID) throws IllegalStateException, NoSuchElementException, SQLException {
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
     * Updates an Event publishing status
     * @param event
     * @return True if everything went smoothly
     * @throws IllegalStateException If called before a database connection is established
     * @throws SQLException
     * @throws SQLTimeoutException
     */
    boolean updateEventPublished(Event event) throws IllegalStateException, SQLException, SQLTimeoutException {
        if (dbConnection == null) throw new IllegalStateException("ALERT: No connection to the database");

        int i = 0;
        String eventType = this.getEventType(event.getEventID());
        PreparedStatement updateEventPublishedStatement = dbConnection.prepareStatement(String.format(UPDATE_EVENT_PUBLISHED, eventType)); // HACK as explained in getEvent
        updateEventPublishedStatement.setBoolean(1, event.isPublished()); // published
        updateEventPublishedStatement.setString(2, event.getEventIDAsString()); // eventID
        i = updateEventPublishedStatement.executeUpdate();
        if (i != 1)
            throw new SQLException("ALERT: Error adding updating event!\nSQL INSERT query returned " + i);
        return true;
    }

    /**
     * Updates an Event registered users
     * @param event
     * @return True if everything went smoothly
     * @throws IllegalStateException If called before a database connection is established
     * @throws SQLException
     * @throws SQLTimeoutException
     */
    boolean updateEventRegistrations(Event event) throws IllegalStateException, SQLException, SQLTimeoutException {
        if (dbConnection == null) throw new IllegalStateException("ALERT: No connection to the database");

        int i = 0;
        String eventType = this.getEventType(event.getEventID());
        PreparedStatement updateEventPublishedStatement = dbConnection.prepareStatement(String.format(UPDATE_EVENT_REGISTERED, eventType)); // HACK as explained in getEvent
        Array registeredUUIDDBArray = dbConnection.createArrayOf("VARCHAR", event.getRegisteredUsersAsString().toArray());
        updateEventPublishedStatement.setArray(1, registeredUUIDDBArray); // registeredUsers
        updateEventPublishedStatement.setString(2, event.getEventIDAsString()); // eventID
        i = updateEventPublishedStatement.executeUpdate();
        if (i != 1)
            throw new SQLException("ALERT: Error adding updating event!\nSQL INSERT query returned " + i);
        return true;
    }

    /**
     * Dumps all available Events in the database to an ArrayList
     * @return ArrayList of Event objects
     * @throws IllegalStateException If called before a database connection is established
     * @throws SQLException If a database access error occurs
     */
    public ArrayList<Event> getEvents() throws IllegalStateException, SQLException {
        if (dbConnection == null) throw new IllegalStateException("ALERT: No connection to the database");

        ArrayList <Event> returnEvents = new ArrayList<>();

        Statement categoriesStatement = dbConnection.createStatement();
        ResultSet rs = categoriesStatement.executeQuery(GET_EVENTS_LIST);

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
     * Returns an Event's type given its UUID
     * @param eventID
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
     * @throws SQLException
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
