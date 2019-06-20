package it.unibs.ing.se.model;

import it.unibs.ing.se.DMO.JsonTranslator;
import it.unibs.ing.se.view.Menu;
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

    /**
     * A full user description with all the fields that can be printed
     * @param  menuTranslation object with Menu fields translation to get "pretty" string from.
     *                         Field names such as "genderInput" and similar can so be translated into human readable forms.
     * @return Description string
     */
    public String detailedDescription(JsonTranslator menuTranslation) {
        StringBuilder sb = new StringBuilder();
        sb.append("Username: ").append(this.username).append('\n');
        sb.append(menuTranslation.getTranslation("genderInput")).append(": ").append(gender);
        if (age != 0) {
            sb.append('\n').append(menuTranslation.getTranslation("ageInput")).append(": ").append(age);
        }
        if (favoriteCategories != null) {
            sb.append('\n').append(menuTranslation.getTranslation("favoriteCategoriesPrint")).append(":");
            for (int i = 0; i < favoriteCategories.length; i++) {
                ArrayList<String> catDescription = Menu.getCategoryDescription(favoriteCategories[i]);
                sb.append('\n').append(i + 1).append(") ");
                sb.append(catDescription.get(0)).append('\n'); // Category name
                sb.append('\t').append(catDescription.get(1)); // Category description
            }
        }
        return sb.toString();
    }

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
