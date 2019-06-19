package model;

import model.fields.Sex;

import java.util.Arrays;

/**
 * Contains basic user parameters
 */
public class UserCore {
    protected String username;
    protected String hashedPassword;
    protected Sex gender;
    protected Integer age; // age defaults to 0 if the user do not set this field
    protected String[] favoriteCategories; // Can be null

    /**
     * @param username chosen username
     * @param hashedPassword chosen password, already hashed
     * @param gender Sex object related to user's sex
     * @param age User's age
     */
    public UserCore(String username, String hashedPassword, Sex gender, Integer age, String[] favoriteCategories) {
        this.username = username;
        this.hashedPassword = hashedPassword;
        this.gender = gender;
        this.age = age;
        this.favoriteCategories = favoriteCategories;
    }

    public String getUsername() {
        return username;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public Sex getGender() {
        return gender;
    }

    public Integer getAge() {
        return age;
    }

    public String[] getFavoriteCategories() {
        return favoriteCategories;
    }

    @Override
    public String toString() {
        return "UserCore{" +
                "username='" + username + '\'' +
                ", hashedPassword='" + hashedPassword + '\'' +
                ", gender=" + gender +
                ", age=" + age +
                ", favoriteCategories=" + Arrays.toString(favoriteCategories) +
                '}';
    }
}
