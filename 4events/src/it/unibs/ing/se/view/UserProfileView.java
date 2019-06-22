package it.unibs.ing.se.view;

import it.unibs.ing.se.DMO.Connector;
import it.unibs.ing.se.DMO.JsonTranslator;
import it.unibs.ing.se.model.User;
import it.unibs.ing.se.model.UserCore;
import it.unibs.ing.se.model.fields.Sex;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class UserProfileView implements PrintableInterface<UserCore> {
    private JsonTranslator translation;
    private User currentUser;

    public UserProfileView(UUID currentUserID) {
        this.translation = JsonTranslator.getInstance();
        try {
            currentUser = Connector.getInstance().getUser(currentUserID);
        } catch (SQLException e) {
            System.err.println(translation.getTranslation("SQLError"));
            System.exit(1);
        }
    }

    @Override
    public void print() {
        StringBuilder sb = new StringBuilder();
        sb.append("Username: ").append(currentUser.getUsername()).append('\n');
        sb.append(translation.getTranslation("genderInput")).append(": ").append(currentUser.getGender());
        if (currentUser.getAge() != 0) {
            sb.append('\n').append(translation.getTranslation("ageInput")).append(": ").append(currentUser.getAge());
        }
        String[] favoriteCategories = currentUser.getFavoriteCategories();
        if (favoriteCategories != null) {
            sb.append('\n').append(translation.getTranslation("favoriteCategoriesPrint")).append(":");
            for (int i = 0; i < favoriteCategories.length; i++) {
                sb.append('\n').append(i + 1).append(") ");
                sb.append(translation.getName(favoriteCategories[i])).append("\n\t");
                sb.append(translation.getDescr(favoriteCategories[i]));
            }
        }
        System.out.println(sb);
    }

    @Override
    public UserCore parseInput() {
        String username = currentUser.getUsername();
        String hashedPassword = currentUser.getHashedPassword();
        Integer age = currentUser.getAge();
        Sex gender = currentUser.getGender();

        ArrayList<String> favoriteCategories = new ArrayList<>(Arrays.asList(currentUser.getFavoriteCategories()));

        Character userInput;
        do {
            userInput = InputManager.inputChar(translation.getTranslation("ageChange"), true);
            if (userInput != null && userInput != 'S' && userInput != 'N')
                userInput = null;
        } while (userInput == null);
        if (userInput == 'S') {
            age = InputManager.inputInteger(translation.getTranslation("ageInput"), true);
            age = (age == null ? 0 : age); // age defaults to 0
        }
        do {
            userInput = InputManager.inputChar(translation.getTranslation("favoriteCategoriesChange"), true);
            if (userInput != null && userInput != 'S' && userInput != 'N')
                userInput = null;
        } while (userInput == null);
        if (userInput == 'S') {
            FavoriteCategoriesView favoriteCategoriesView = new FavoriteCategoriesView();
            favoriteCategoriesView.print();
            favoriteCategories = favoriteCategoriesView.parseInput();
        }

        String[] favoriteCategoriesArray = favoriteCategories.toArray(new String[favoriteCategories.size()]);

        return new UserCore(username, hashedPassword, gender, age, favoriteCategoriesArray);
    }
}
