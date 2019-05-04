package impl;

import java.sql.*;
import java.util.*;

class Connector {
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
            ResultSet rs = categoriesStatement.executeQuery("select * from categories");

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
        String query = "SELECT * FROM categories WHERE event_type LIKE ?";

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
     * TODO method documentation
     */
    void saveEventToDb (Event event) throws SQLException, SQLTimeoutException {
        if (dbConnection == null) throw new IllegalStateException("ALERT: No connection to the database");

        int parametersNumber = 0;
        StringBuilder query = new StringBuilder();
        query.append("INSERT INTO ");
        query.append(event.getCatName()).append(" (");
        query.append("eventID, creatorID, category, catName, catDescription"); // Event private fields shouldn't be retrieved via getAttributesName // TODO this should probably have its own method in Event
        parametersNumber += 5; // Number of Event private fields // TODO same here

        LinkedHashMap<String, String> setAttributes = event.getNonNullAttributesWithDBString();

        Iterator iterator = setAttributes.entrySet().iterator(); // Get an iterator for our map

        while(iterator.hasNext()) {
            Map.Entry entry = (Map.Entry)iterator.next(); // Casts the iterated item to a Map Entry to use it as such
            query.append(", ").append(entry.getKey());
            parametersNumber++;
        }
        query.append(") VALUES (?"); // Notice the question mark
        for (int i = 0; i < parametersNumber - 1; i++) // Minus one since the first question mark is present in the above line
            query.append(", ?");
        query.append(")"); // We now have the query that can be passed to PreparedStatement
    }
}
