package it.unibs.ing.se.view;

import it.unibs.ing.se.DMO.Connector;
import it.unibs.ing.se.DMO.JsonTranslator;
import it.unibs.ing.se.view.inputwrappers.EventInput;
import it.unibs.ing.se.view.inputwrappers.EventInputFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class NewEventView implements PrintableInterface<EventInput> {
    private JsonTranslator translation;
    private ArrayList<String> categories;

    public NewEventView() {
        this.translation = JsonTranslator.getInstance();
        try {
            this.categories = Connector.getInstance().getCategories();
        } catch (SQLException e) {
            System.err.println(translation.getTranslation("SQLError"));
            System.exit(1);
        }
    }

    @Override
    public void print() {
        StringBuilder sb = new StringBuilder();
        sb.append(translation.getTranslation("categoryList")).append('\n');

        String cat;
        String catName;
        String catDescr;
        for (int i = 0; i < categories.size(); i++) {
            cat = categories.get(i);
            catName = translation.getName(cat);
            catDescr = translation.getDescr(cat);
            // Numbers printed below will be 1 based, so to select the right category user input hast to be decremented by one
            sb.append(i + 1).append(") ").append(catName).append("\n     ");
            sb.append(catDescr).append('\n');
        }
        System.out.println(sb);

    }

    @Override
    public EventInput parseInput() {
        Integer userSelection = InputManager.inputInteger(translation.getTranslation("userSelection"), false);
        if (userSelection == null || userSelection <= 0 || userSelection > categories.size()) {
            System.out.println(translation.getTranslation("invalidUserSelection"));
            return null;
        }

        EventInputFactory eFactory = new EventInputFactory();
        EventInput event = eFactory.createEvent(categories.get(userSelection - 1));

        LinkedHashMap<String, Class<?>> eventFieldsMap = event.getAttributesWithType();

        Iterator iterator = eventFieldsMap.entrySet().iterator(); // Get an iterator for our map

        while(iterator.hasNext()) {
            Map.Entry entry = (Map.Entry)iterator.next(); // Casts the iterated item to a Map Entry to use it as such

            boolean validUserInput = false;
            do {
                String inputDescription = translation.getName((String) entry.getKey());
                Object userInput = InputManager.genericInput(inputDescription, (Class) entry.getValue(), true);
                if (userInput == null) {
                    validUserInput = event.isOptional((String) entry.getKey());
                } else {
                    event.setAttribute((String) entry.getKey(), userInput);
                    validUserInput = true;
                }
            } while (!validUserInput);
        }

        return event;
    }
}
