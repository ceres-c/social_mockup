package impl;

import impl.fields.Sex;

import java.util.Objects;
import java.util.UUID;

public class User {
    private UUID userID;
    private String username;
    private String hashedPassword;
    private Sex gender;

    /**
     * Creates an User object given username and hashedPassword
     * This should be used to create new users
     * @param username
     * @param hashedPassword
     */
    public User(String username, String hashedPassword, Sex gender) {
        this.username = username;
        this.hashedPassword = hashedPassword;
        this.userID = UUID.randomUUID();
        this.gender = gender;
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

    public void setGender(Sex gender) { this.gender = gender; }

    public UUID getUserID() { return userID; }

    public String getUserIDAsString() {
        return userID.toString();
    }

    public String getUsername() {
        return username;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public Sex getGender() { return gender; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return getUserID().equals(user.getUserID()) &&
                getUsername().equals(user.getUsername()) &&
                getHashedPassword().equals(user.getHashedPassword()) &&
                getGender().equals(user.getGender());
    }
}
