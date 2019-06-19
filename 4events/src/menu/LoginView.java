package menu;

import DMO.JsonTranslator;
import model.CryptoHelper;

import java.util.ArrayList;

public class LoginView implements PrintableInterface<ArrayList<String>> {
    JsonTranslator menuTranslation;
    CryptoHelper crypto;

    public LoginView(JsonTranslator translation, CryptoHelper cryptoHelper) {
        this.menuTranslation = translation;
        this.crypto = cryptoHelper;
    }

    @Override
    public void print() {
        System.out.println(menuTranslation.getTranslation("loginPrompt"));
    }

    @Override
    public ArrayList<String> parseInput() {
        String username;
        char[] password;
        ArrayList<String> userData = new ArrayList<>();

        while ((username = InputManager.inputString("Username", true)) == null);
        while ((password = InputManager.inputPassword("Password")) == null);

        byte[] salt = crypto.charArrayToByteArray(username.toCharArray());
        String hashedPassword = crypto.SHA512PasswordHash(password, salt);
        java.util.Arrays.fill(password, ' '); // It will still be somewhere in memory due to Java's Almighty Garbage Collector (TM), but at least we tried.

        userData.add(username);
        userData.add(hashedPassword);

        return userData;
    }
}
