package it.unibs.ing.se.view;

import it.unibs.ing.se.DMO.JsonTranslator;
import it.unibs.ing.se.view.commands.DashboardCommand;

public class DashboardView implements PrintableInterface<DashboardCommand> {
    JsonTranslator translation;
    private DashboardCommand[] dashCommands = DashboardCommand.values();

    public DashboardView () {
        this.translation = JsonTranslator.getInstance();
    }

    @Override
    public void print() {
        StringBuilder sb = new StringBuilder();
        sb.append(translation.getTranslation("welcomeDashboard")).append('\n');
        for (int i = 1; i < dashCommands.length; i++) {
            sb.append(i).append(") ");
            sb.append(translation.getTranslation(dashCommands[i].name())).append('\n');
        }
        System.out.print(sb); // No trailing newline as it was already added on above line
    }

    @Override
    public DashboardCommand parseInput() {
        Integer userSelection = InputManager.inputInteger(translation.getTranslation("userSelection"), false);

        if (userSelection == null || userSelection < 0 || userSelection > dashCommands.length - 1)
            return DashboardCommand.INVALID;

        return dashCommands[userSelection]; // 0 is INVALID and user available commands start at 1
    }
}
