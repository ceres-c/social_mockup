package impl;

import impl.fields.Sex;

import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

class Connector {
    private final static String SELECT_USER = "SELECT * FROM public.users WHERE username LIKE ?";
    private final static String INSERT_USER = "INSERT INTO public.users values (?, ?, ?)";
    private final static String GET_CATEGORIES_QUERY = "select * from public.categories";
    private final static String GET_CATEGORY_DESCRIPTION = "SELECT * FROM public.categories WHERE event_type LIKE ?";

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
            returnUser.setUsername(rs.getString(1));
            returnUser.setHashedPassword(rs.getString(2));
            returnUser.setUserID(UUID.fromString(rs.getString(3)));
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
        String query = INSERT_USER;

        int i = 0;
        try {
            PreparedStatement insertUserStatement = dbConnection.prepareStatement(query);
            insertUserStatement.setString(1, user.getUsername());
            insertUserStatement.setString(2, user.getHashedPassword());
            insertUserStatement.setString(3, user.getUserID());
            i = insertUserStatement.executeUpdate();
        } catch(java.sql.SQLException e) {
            if (e.getMessage().contains("duplicate key value")) {
                throw new IllegalArgumentException("ALERT: Duplicate user " + user.getUsername());
            }
            System.out.println("ALERT: Failed to insert user in database");
            e.printStackTrace();
        }
        if (i != 1)
            throw new SQLException("ALERT: Error adding user to the database!\n" + "SQL INSERT query returned " + i);
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

        ArrayList<String> returnAList = new ArrayList<>();
        try {
            Statement categoriesStatement = dbConnection.createStatement();
            ResultSet rs = categoriesStatement.executeQuery(GET_CATEGORIES_QUERY);

            if (!rs.next()) { // rs.next() returns false when the query has no results
                throw new NoSuchElementException("ALERT: No categories in the database");
            } else {
                do {
                    returnAList.add(rs.getString(1));
                } while (rs.next());
            }
        } catch(java.sql.SQLException e) {
            System.out.println("ALERT: Failed getting categories from database!");
            e.printStackTrace();
        }
        return returnAList;
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

        ArrayList<String> returnAList = new ArrayList<>();
        try {
            PreparedStatement categoryDescrStatement = dbConnection.prepareStatement(query);
            categoryDescrStatement.setString(1, categoryName);
            ResultSet rs = categoryDescrStatement.executeQuery();

            if (!rs.next()) { // rs.next() returns false when the query has no results
                throw new NoSuchElementException("ALERT: Category " + categoryName + " does not exist in the database");
            } else {
                do {
                    returnAList.add(rs.getString(2));
                    returnAList.add(rs.getString(3));
                } while (rs.next());
            }
        } catch(java.sql.SQLException e) {
            System.out.println("ALERT: Failed getting category description!");
            e.printStackTrace();
        }
        return returnAList;
    }

    /**
     * Inserts an Event into the database in the proper table given an Event-like object
     * @return  true if everything went smoothly
     * @throws IllegalStateException If called before a database connection is established
     * @throws SQLException If a database access error occurs; this method is called on a closed PreparedStatement
     *                      or the SQL statement returns a ResultSet object
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
        query.append(event.getCatName()).append(" ("); // category name. Eg: "INSERT INTO public.soccer_game ("
        // Event private fields can't be retrieved via getNonNullAttributesWithValue, also, these names are common to all categories
        query.append("eventID, creatorID, eventType");
        parametersNumber += 3; // Number of Event private fields

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
        addEventStatement.setString(parameterIndex, event.getEventID()); // Event private fields here
        parameterIndex++;
        addEventStatement.setString(parameterIndex, event.getCreatorID()); // same
        parameterIndex++;
        addEventStatement.setString(parameterIndex, event.getEventType()); // same
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
}
