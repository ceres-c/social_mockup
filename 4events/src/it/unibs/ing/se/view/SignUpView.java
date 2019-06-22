package it.unibs.ing.se.view;

import it.unibs.ing.se.DMO.JsonTranslator;
import it.unibs.ing.se.controller.helpers.CryptoHelper;
import it.unibs.ing.se.model.UserCore;
import it.unibs.ing.se.model.fields.Sex;

import java.util.ArrayList;

public class SignUpView implements PrintableInterface<UserCore> {
    private JsonTranslator translation;
    private FavoriteCategoriesView favoriteCategoriesView;

    public SignUpView() {
        this.translation = JsonTranslator.getInstance();
        favoriteCategoriesView = new FavoriteCategoriesView();
    }

    @Override
    public void print() {
        System.out.println(translation.getTranslation("signUpPrompt"));
    }

    @Override
    public UserCore parseInput() {
        String username;
        char[] password;
        Sex gender;
        Integer age;

        while ((username = InputManager.inputString("Username", true)) == null);
        while ((password = InputManager.inputPassword("Password")) == null);
        byte[] salt = CryptoHelper.charArrayToByteArray(username.toCharArray());
        String hashedPassword = CryptoHelper.SHA512PasswordHash(password, salt);
        java.util.Arrays.fill(password, ' '); // It will still be somewhere in memory due to Java's Almighty Garbage Collector (TM), but at least we tried.

        while ((gender = InputManager.sexInput(translation.getTranslation("genderInput"), true)) == null);
        age = InputManager.inputInteger(translation.getTranslation("ageInput"), true);
        age = (age == null ? 0 : age); // age defaults to 0

        favoriteCategoriesView.print();
        ArrayList<String> favoriteCategories = favoriteCategoriesView.parseInput();
        String[] favoriteCategoriesArray = favoriteCategories.toArray(new String[favoriteCategories.size()]);

        return new UserCore(username, hashedPassword, gender, age, favoriteCategoriesArray);
    }
}
