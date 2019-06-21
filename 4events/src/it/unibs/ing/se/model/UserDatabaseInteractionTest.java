package it.unibs.ing.se.model;

import it.unibs.ing.se.DMO.Connector;
import it.unibs.ing.se.DMO.JsonConfigReader;
import it.unibs.ing.se.controller.helpers.CryptoHelper;
import it.unibs.ing.se.model.fields.Sex;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserDatabaseInteractionTest {
    private final String username = "user001";
    private final char[] password = {'i', 'l', 'o', 'v', 'e', 'y', 'o', 'u'}; // rockyou.txt docet
    byte[] salt = CryptoHelper.charArrayToByteArray(username.toCharArray());
    String hashedPassword = CryptoHelper.SHA512PasswordHash(password, salt);
    private final Sex gender = new Sex("M");
    private final Integer age = 25;
    private UserTest user1 = new UserTest(username, hashedPassword, gender, age, null); // favooriteCategories not specified

    private static final String CONFIG_JSON_PATH = "config.json";
    Path configPath = Paths.get(CONFIG_JSON_PATH);
    JsonConfigReader config = new JsonConfigReader(configPath.toString());
    Connector myConnector;

    UserDatabaseInteractionTest () {
        try {
            myConnector = Connector.getInstance(config.getDBURL(), config.getDBUser(), config.getDBPassword());
        } catch (SQLException e) {
            System.out.println("ALERT: Error establishing a database connection!");
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Test
    void userSave() {
        try {
            assertTrue(myConnector.insertUser(user1));
        } catch (SQLException e) {
            assertTrue(false); // The test fails if an exception is thrown
        }
    }

    @Test
    void userGet() {
        try {
            myConnector.login(username, hashedPassword);
            assertTrue(true); // If no exception is thrown this test is fine
        } catch (SQLException e) {
            assertTrue(false); // The test fails if an exception is thrown
        }
    }

    /*
    @Test
    void userEquals() {
        System.out.println(user1.getUserIDAsString());
        User dbUser = null;
        try {
            dbUser = myConnector.login(username, hashedPassword); // TODO get User object with database getter
            //System.out.println(dbUser.getUserID());
        } catch (SQLException e) {
            assertTrue(false); // The test fails if an exception is thrown
        }
        assertTrue(user1.equals(dbUser));
    }
    */


    /**
     * This subclass is the only way to work around UUID randomness.
     * It extends User and override needed methods for this test so that UUID is fixed.
     */
    class UserTest extends User {
        UUID userIdTest = UUID.fromString("123e4567-e89b-12d3-a456-556642440000");

        public UserTest(String username, String hashedPassword, Sex gender, Integer age, String[] favoriteCategories) {
            super(username, hashedPassword, gender, age, favoriteCategories);
        }

        @Override
        public UUID getUserID() {
            return userIdTest;
        }

        @Override
        public String getUserIDAsString() {
            return userIdTest.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null) return false;
            User user = (User) o;
            return getUserID().equals(user.getUserID()) &&
                    getUsername().equals(user.getUsername()) &&
                    getHashedPassword().equals(user.getHashedPassword()) &&
                    getGender().equals(user.getGender());
        }
    }
}