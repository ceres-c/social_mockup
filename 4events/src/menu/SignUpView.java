package menu;

import DMO.JsonTranslator;
import model.CryptoHelper;
import model.UserCore;
import model.fields.Sex;

import java.util.ArrayList;

public class SignUpView implements PrintableInterface<UserCore> {
    JsonTranslator menuTranslation;
    CryptoHelper crypto;
    FavoriteCategoriesView favoriteCategoriesView;

    public SignUpView(JsonTranslator menuTranslation, JsonTranslator eventTranslation, DMO.Connector dbConnection, CryptoHelper cryptoHelper) {
        this.menuTranslation = menuTranslation;
        this.crypto = cryptoHelper;
        favoriteCategoriesView = new FavoriteCategoriesView(menuTranslation, eventTranslation, dbConnection);
    }

    @Override
    public void print() {
        System.out.println(menuTranslation.getTranslation("signUpPrompt"));
    }

    @Override
    public UserCore parseInput() {
        String username;
        char[] password;
        Sex gender;
        Integer age;

        while ((username = InputManager.inputString("Username", true)) == null);
        while ((password = InputManager.inputPassword("Password")) == null);
        byte[] salt = crypto.charArrayToByteArray(username.toCharArray());
        String hashedPassword = crypto.SHA512PasswordHash(password, salt);
        java.util.Arrays.fill(password, ' '); // It will still be somewhere in memory due to Java's Almighty Garbage Collector (TM), but at least we tried.

        while ((gender = Sex.sexInput(menuTranslation.getTranslation("genderInput"), true)) == null);
        age = InputManager.inputInteger(menuTranslation.getTranslation("ageInput"), true);
        age = (age == null ? 0 : age); // age defaults to 0

        favoriteCategoriesView.print();
        ArrayList<String> favoriteCategories = favoriteCategoriesView.parseInput();
        String[] favoriteCategoriesArray = favoriteCategories.toArray(new String[favoriteCategories.size()]);

        UserCore newUser = new UserCore(username, hashedPassword, gender, age, favoriteCategoriesArray);
        return newUser;
    }
}
