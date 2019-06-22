package it.unibs.ing.se.model.fields;

import java.util.UUID;

public class OptionalCost {
    private UUID costID;
    private Integer costAmount;

    public OptionalCost(UUID costID, Integer costAmount) {
        if (costAmount < 0)
            throw new IllegalArgumentException("Illegal input: " + costAmount);
        this.costID = costID;
        this.costAmount = costAmount;
    }

    public UUID getCostID() { return costID; }

    public Integer getCostAmount() { return costAmount; }

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
