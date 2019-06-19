package controller;

import DMO.Connector;
import DMO.JsonTranslator;
import controller.LoginSignupController;
import menu.LoginSignupView;
import menu.LoginView;
import menu.SignUpView;
import model.CryptoHelper;
import model.User;

public class LoginHelper {
    JsonTranslator menuTranslation;
    JsonTranslator eventTranslation;
    Connector dbConnection;
    CryptoHelper crypto;

    public LoginHelper(JsonTranslator menuTranslation, JsonTranslator eventTranslation, Connector dbConnection) {
        this.menuTranslation = menuTranslation;
        this.dbConnection = dbConnection;
        this.crypto = new CryptoHelper();
    }

    public User login() {
        User newUser;

        LoginSignupController loginSignupController = new LoginSignupController(menuTranslation, dbConnection);
        LoginSignupView loginSignupView = new LoginSignupView(menuTranslation);
        loginSignupView.print();
        Integer userInput = loginSignupView.parseInput();
        if (userInput == 1) {
            // Login
            LoginView login = new LoginView(menuTranslation, crypto);
            login.print();
            newUser = loginSignupController.login(login.parseInput());
        } else {
            // Sign Up
            SignUpView signUp = new SignUpView(menuTranslation, eventTranslation, dbConnection, crypto);
            signUp.print();
            newUser = loginSignupController.signup(signUp.parseInput());
            // Sign Up
        }
        return newUser;
    }
}
