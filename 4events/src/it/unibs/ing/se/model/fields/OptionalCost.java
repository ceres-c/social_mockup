package it.unibs.ing.se.model.fields;

import it.unibs.ing.se.view.InputManager;

import java.util.UUID;

public class OptionalCost {
    UUID costID;
    Integer costAmount;

    public OptionalCost(UUID costID, Integer costAmount) {
        this.costID = costID;
        this.costAmount = costAmount;
    }

    public UUID getCostID() { return costID; }

    public Integer getCostAmount() { return costAmount; }

    static public OptionalCost optionalCostInput (String inputDescription, boolean inline) {
        Character userInput;
        Integer amount = 0;
        do {
            userInput= InputManager.inputChar(inputDescription + " (S|N)", inline);
            if (userInput == null) {
                return null;
            } else if (userInput == 'S') {
                amount = InputManager.inputInteger("Cost (€)", true);
            }
        } while (! (userInput == 'S' || userInput == 'N'));
        if (userInput == 'S') {
            return new OptionalCost(UUID.randomUUID(), amount);
        } else {
            return null;
        }
    }

    /**
     * This stripped down toString method allows to print these costs as any other field,
     * while it is not as complete as it should be.
     * @return String with only the cost of this optional cost
     */
    @Override
    public String toString() {
        return costAmount.toString() + " €";
    }

    /*
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Cost ID: ").append(costID.toString()).append("\n");
        sb.append("Cost Amount: ").append(costAmount).append("\n");
        return sb.toString();
    }
    */
}
