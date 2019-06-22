package it.unibs.ing.se.helpers;

import it.unibs.ing.se.controller.LoginSignupController;
import it.unibs.ing.se.view.LoginSignupView;
import it.unibs.ing.se.view.LoginView;
import it.unibs.ing.se.view.SignUpView;

import java.util.UUID;

/**
 * Helper class to manage user login and signup.
 * It's a pseudo-it.unibs.ing.se.controller, but does not implement ControllerInterface as login returns a User object,
 * while other controllers do not return anything.
 */
public class LoginHelper {
    /**
     * Prompts the user to sign up or login and then performs signup/login
     * @return User object
     */
    public UUID login() {
        UUID newUser;

        LoginSignupController loginSignupController = new LoginSignupController();
        LoginSignupView loginSignupView = new LoginSignupView();
        loginSignupView.print();
        Integer userInput = loginSignupView.parseInput();
        if (userInput == 1) {
            // Login
            LoginView login = new LoginView();
            login.print();
            newUser = loginSignupController.login(login.parseInput());
        } else {
            // Sign Up
            SignUpView signUp = new SignUpView();
            signUp.print();
            newUser = loginSignupController.signup(signUp.parseInput());
            // Sign Up
        }
        return newUser;
    }
}
