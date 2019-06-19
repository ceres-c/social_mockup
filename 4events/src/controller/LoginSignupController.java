package controller;

import DMO.Connector;
import DMO.JsonTranslator;
import model.User;
import model.UserCore;
import model.fields.Sex;

import java.sql.SQLException;
import java.util.ArrayList;

public class LoginSignupController {
    JsonTranslator menuTranslation;
    Connector dbConnection;

    public LoginSignupController(JsonTranslator translation, Connector dbConnection) {
        this.menuTranslation = translation;
        this.dbConnection = dbConnection;
    }

    public User login(ArrayList<String> userData) {
        String username = userData.get(0);
        String hashedPassword = userData.get(1);

        User userFromDb = null;
        try {
            userFromDb = dbConnection.getUser(username, hashedPassword);
        } catch (IllegalArgumentException e) {
            System.err.println(menuTranslation.getTranslation("loginError"));
            return null; // CHECK FOR NULL-OBJECT!
        } catch (SQLException e) {
            System.err.println(menuTranslation.getTranslation("SQLError"));
            System.exit(1);
        }
        return userFromDb;
    }

    public User signup(UserCore userCore) {
        String username = userCore.getUsername();
        String hashedPassword = userCore.getHashedPassword();
        Sex gender = userCore.getGender();
        Integer age = userCore.getAge();
        String[] favoriteCategories = userCore.getFavoriteCategories();

        User newUser = new User(username, hashedPassword, gender, age, favoriteCategories);
        try {
            dbConnection.insertUser(newUser);
        } catch (IllegalArgumentException e) {
            System.err.println(menuTranslation.getTranslation("duplicateUser"));
            return null; // CHECK FOR NULL-OBJECT!
        } catch (SQLException e) {
            System.err.println(menuTranslation.getTranslation("SQLError"));
            System.exit(1);
        }
        return newUser;
    }
}
