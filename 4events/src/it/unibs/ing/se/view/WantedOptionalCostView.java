package it.unibs.ing.se.view;

import it.unibs.ing.se.DMO.Connector;
import it.unibs.ing.se.DMO.JsonTranslator;
import it.unibs.ing.se.model.Event;
import it.unibs.ing.se.model.fields.OptionalCost;

import java.sql.SQLException;
import java.util.*;

public class WantedOptionalCostView implements PrintableInterface<LinkedHashMap<String, OptionalCost>> {
    private JsonTranslator menuTranslation;
    private JsonTranslator eventTranslation;
    private Connector dbConnection;
    private LinkedHashMap<String, OptionalCost> allCostsMap;
    private ArrayList<String> mapKeys;
    private Event event;

    public WantedOptionalCostView (UUID eventID) {
        dbConnection = Connector.getInstance();
        this.menuTranslation = new JsonTranslator(JsonTranslator.MENU_JSON_PATH);
        this.eventTranslation = new JsonTranslator(JsonTranslator.EVENT_JSON_PATH);
        try {
            this.event = dbConnection.getEvent(eventID);
        } catch (SQLException e) {
            System.err.println(menuTranslation.getTranslation("SQLError"));
            System.exit(1);
        }
        this.allCostsMap = event.getOptionalCosts();
        if (allCostsMap != null)
            this.mapKeys = new ArrayList<>(allCostsMap.keySet());

    }

    @Override
    public void print() {
        if (allCostsMap == null)
            return;
        StringBuilder sb = new StringBuilder();

        sb.append(menuTranslation.getTranslation("optionalCostsList")).append('\n');

        for (int i = 0; i < mapKeys.size(); i++) {
            String costName = mapKeys.get(i);
            OptionalCost cost = allCostsMap.get(costName);
            sb.append(i + 1).append(") ");
            sb.append(eventTranslation.getName(costName)).append(" - ");
            sb.append(cost.getCostAmount()).append("€\n");
            sb.append(eventTranslation.getDescr(costName)).append('\n');
        }
        System.out.println(sb);
    }

    @Override
    public LinkedHashMap<String, OptionalCost> parseInput() {
        if (allCostsMap == null)
            return null;
        ArrayList<Integer> userNumbers = InputManager.inputNumberSequence(menuTranslation.getTranslation("optionalCostsSelection"), true);
        LinkedHashMap<String, OptionalCost> selectedCosts = new LinkedHashMap<>();
        if (userNumbers == null) {
            System.out.println(menuTranslation.getTranslation("optionalCostNoSelection"));
            return null;
        }
        for (Integer number : userNumbers)
            if (number - 1 >= mapKeys.size() || number - 1 < 0) { // Categories are printed starting from number 1
                System.err.println(menuTranslation.getTranslation("invalidFavoriteCategorySelected"));
                return null; // out of bound
            } else {
                String costName = mapKeys.get(number - 1);
                OptionalCost cost = allCostsMap.get(costName);
                selectedCosts.put(costName, cost);
            }

        return selectedCosts;
    }
}
