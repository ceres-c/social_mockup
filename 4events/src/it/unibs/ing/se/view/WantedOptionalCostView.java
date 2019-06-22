package it.unibs.ing.se.view;

import it.unibs.ing.se.DMO.Connector;
import it.unibs.ing.se.DMO.JsonTranslator;
import it.unibs.ing.se.model.Event;
import it.unibs.ing.se.model.fields.OptionalCost;

import java.sql.SQLException;
import java.util.*;

public class WantedOptionalCostView implements PrintableInterface<LinkedHashMap<String, OptionalCost>> {
    private JsonTranslator translation;
    private LinkedHashMap<String, OptionalCost> allCostsMap;
    private ArrayList<String> mapKeys;
    private Event event;

    public WantedOptionalCostView (UUID eventID) {
        this.translation = JsonTranslator.getInstance();
        try {
            this.event = Connector.getInstance().getEvent(eventID);
        } catch (SQLException e) {
            System.err.println(translation.getTranslation("SQLError"));
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

        sb.append(translation.getTranslation("optionalCostsList")).append('\n');

        for (int i = 0; i < mapKeys.size(); i++) {
            String costName = mapKeys.get(i);
            OptionalCost cost = allCostsMap.get(costName);
            sb.append(i + 1).append(") ");
            sb.append(translation.getName(costName)).append(" - ");
            sb.append(cost.getCostAmount()).append("€\n");
            sb.append(translation.getDescr(costName)).append('\n');
        }
        System.out.println(sb);
    }

    @Override
    public LinkedHashMap<String, OptionalCost> parseInput() {
        if (allCostsMap == null)
            return null;
        ArrayList<Integer> userNumbers = InputManager.inputNumberSequence(translation.getTranslation("optionalCostsSelection"), true);
        LinkedHashMap<String, OptionalCost> selectedCosts = new LinkedHashMap<>();
        if (userNumbers == null) {
            System.out.println(translation.getTranslation("optionalCostNoSelection"));
            return null;
        }
        for (Integer number : userNumbers)
            if (number - 1 >= mapKeys.size() || number - 1 < 0) { // Categories are printed starting from number 1
                System.err.println(translation.getTranslation("invalidFavoriteCategorySelected"));
                return null; // out of bound
            } else {
                String costName = mapKeys.get(number - 1);
                OptionalCost cost = allCostsMap.get(costName);
                selectedCosts.put(costName, cost);
            }

        return selectedCosts;
    }
}
