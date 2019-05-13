package impl;

import java.util.UUID;

public class User {
    private UUID userID;
    private String username;
    private String hashedPassword;

    /**
     * Creates an User object given username and hashedPassword
     * This should be used to create new users
     * @param username
     * @param hashedPassword
     */
    public User(String username, String hashedPassword) {
        this.userID = UUID.randomUUID();
        this.username = username;
        this.hashedPassword = hashedPassword;
    }

    /**
     * Creates an empty User object to be filled with data from the database.
     * This should be used to log in existing users
     */
    public User() {}

    public void setUserID(UUID userID) {
        this.userID = userID;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setHashedPassword(String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    public String getUserID() {
        return userID.toString();
    }

    public String getUsername() {
        return username;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

}
