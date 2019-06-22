package it.unibs.ing.se.view;

import it.unibs.ing.se.DMO.JsonTranslator;
import it.unibs.ing.se.controller.helpers.CryptoHelper;

import java.util.ArrayList;

public class LoginView implements PrintableInterface<ArrayList<String>> {
    JsonTranslator translation;

    public LoginView() {
        this.translation = JsonTranslator.getInstance();
    }

    @Override
    public void print() {
        System.out.println(translation.getTranslation("loginPrompt"));
    }

    @Override
    public ArrayList<String> parseInput() {
        String username;
        char[] password;
        ArrayList<String> userData = new ArrayList<>();

        while ((username = InputManager.inputString("Username", true)) == null);
        while ((password = InputManager.inputPassword("Password")) == null);

        byte[] salt = CryptoHelper.charArrayToByteArray(username.toCharArray());
        String hashedPassword = CryptoHelper.SHA512PasswordHash(password, salt);
        java.util.Arrays.fill(password, ' '); // It will still be somewhere in memory due to Java's Almighty Garbage Collector (TM), but at least we tried.

        userData.add(username);
        userData.add(hashedPassword);

        return userData;
    }
}
