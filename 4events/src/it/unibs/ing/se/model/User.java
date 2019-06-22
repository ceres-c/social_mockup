package it.unibs.ing.se.model;

import it.unibs.ing.se.model.fields.Sex;

import java.util.*;

public class User extends UserCore {
    private UUID userID;

    /**
     * This should be used to create new users since it generates a random userID
     * @param username chosen username
     * @param hashedPassword chosen password, already hashed
     * @param gender Sex object related to user's sex
     * @param age User's age
     */
    public User(String username, String hashedPassword, Sex gender, Integer age, String[] favoriteCategories) {
        super(username, hashedPassword, gender, age, favoriteCategories);
        this.userID = UUID.randomUUID();
    }

    /**
     * This should be used to login users already present in the database
     * @param username chosen username
     * @param hashedPassword chosen password, already hashed
     * @param userID UUID object with UUID of a user already saved in the database
     * @param gender Sex object related to user's sex
     * @param age User's age
     */
    public User(String username, String hashedPassword, UUID userID, Sex gender, Integer age, String[] favoriteCategories) {
        super(username, hashedPassword, gender, age, favoriteCategories);
        this.userID = userID;
    }

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

    public Integer getAge() { return age; }

    public String[] getFavoriteCategories() { return favoriteCategories; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return getUserID().equals(user.getUserID()) &&
                getUsername().equals(user.getUsername()) &&
                getHashedPassword().equals(user.getHashedPassword()) &&
                getGender().equals(user.getGender()) &&
                Objects.equals(getAge(), user.getAge()) &&
                Objects.equals(getFavoriteCategories(), user.getFavoriteCategories());
    }

    @Override
    public String toString() {
        return "User{" +
                "userID=" + userID +
                ", username='" + username + '\'' +
                ", hashedPassword='" + hashedPassword + '\'' +
                ", gender=" + gender +
                ", age=" + age +
                ", favoriteCategories=" + Arrays.toString(favoriteCategories) +
                '}';
    }
}
