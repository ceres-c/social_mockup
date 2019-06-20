package it.unibs.ing.se.view;

import it.unibs.ing.se.DMO.Connector;
import it.unibs.ing.se.DMO.JsonTranslator;

import java.util.ArrayList;

public class FavoriteCategoriesView implements PrintableInterface<ArrayList<String>> {
    private JsonTranslator menuTranslation;
    private JsonTranslator eventTranslation;
    private ArrayList<String> availableCategories;

    public FavoriteCategoriesView() {
        this.menuTranslation = new JsonTranslator(JsonTranslator.MENU_JSON_PATH);
        this.eventTranslation = new JsonTranslator(JsonTranslator.EVENT_JSON_PATH);
        try {
            Connector dbConnection = Connector.getInstance();
            availableCategories = dbConnection.getCategories();
        } catch (IllegalStateException e) {
            System.err.println(menuTranslation.getTranslation("SQLError"));
            System.exit(1);
        }
    }

    /**
     * Prints all available categories with names
     */
    @Override
    public void print() {
        StringBuilder sb = new StringBuilder();

        sb.append(menuTranslation.getTranslation("categoryList")).append('\n');
        for (int i = 0; i < availableCategories.size(); i++) {
            sb.append(i + 1).append(") ");
            sb.append(eventTranslation.getName(availableCategories.get(i))).append('\n');
        }
        System.out.println(sb);
    }

    /**
     * Asks the user to choose which category of events he's interested to among available ones.
     * WARNING Can return null object if the user did not select any category
     * @return An ArrayList of String objects containing categories names as saved in database table "categories"
     */
    public ArrayList<String> parseInput () {
        ArrayList<Integer> userNumbers = InputManager.inputNumberSequence(menuTranslation.getTranslation("favoriteCategoriesInput"), true);
        ArrayList<String> selectedCategories = new ArrayList<>();
        if (userNumbers == null) {
            System.out.println(menuTranslation.getTranslation("noFavoriteCategorySelected"));
            return null;
        }
        for (Integer number : userNumbers)
            if (number - 1 >= availableCategories.size() || number <= 0) { // Categories are printed starting from number 1
                System.err.println(menuTranslation.getTranslation("invalidFavoriteCategorySelected"));
                return null; // out of bound
            } else {
                selectedCategories.add(availableCategories.get(number - 1));
            }

        return selectedCategories;
    }
}
