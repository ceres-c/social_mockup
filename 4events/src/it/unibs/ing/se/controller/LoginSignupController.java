package it.unibs.ing.se.controller;

import it.unibs.ing.se.DMO.Connector;
import it.unibs.ing.se.DMO.JsonTranslator;
import it.unibs.ing.se.model.User;
import it.unibs.ing.se.model.UserCore;
import it.unibs.ing.se.model.fields.Sex;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;

class LoginSignupController {
    private JsonTranslator menuTranslation;
    private Connector dbConnection;

    LoginSignupController() {
        this.menuTranslation = new JsonTranslator(JsonTranslator.MENU_JSON_PATH);
        try {
            dbConnection = Connector.getInstance();
        } catch (IllegalStateException e) {
            System.err.println(menuTranslation.getTranslation("SQLError"));
            System.exit(1);
        }
    }

    UUID login(ArrayList<String> userData) {
        String username = userData.get(0);
        String hashedPassword = userData.get(1);

        UUID userID = null;
        try {
            userID = dbConnection.login(username, hashedPassword);
        } catch (IllegalArgumentException e) {
            System.err.println(menuTranslation.getTranslation("loginError"));
            return null; // CHECK FOR NULL-OBJECT!
        } catch (SQLException e) {
            System.err.println(menuTranslation.getTranslation("SQLError"));
            System.exit(1);
        }
        return userID;
    }

    UUID signup(UserCore userCore) {
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
        return newUser.getUserID();
    }
}
