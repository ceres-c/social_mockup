package it.unibs.ing.se.view;

import it.unibs.ing.se.DMO.Connector;
import it.unibs.ing.se.DMO.JsonTranslator;

import it.unibs.ing.se.view.commands.MainCommand;

import java.sql.SQLException;
import java.util.UUID;

public class MainMenuView implements PrintableInterface<MainCommand> {
    private JsonTranslator translation;
    private Connector dbConnection;
    private UUID currentUserID;
    private MainCommand[] mainCommands = MainCommand.values();

    /**
     * @param currentUserID User object of current user
     */
    public MainMenuView(UUID currentUserID) {
        this.translation = JsonTranslator.getInstance();
        this.dbConnection = dbConnection = Connector.getInstance();
        this.currentUserID = currentUserID;
    }

    /**
     * Prints available options (took from MainCommands)
     */
    public void print() {
        String username = null;
        int unreadNotificationsNum = 0;
        try {
            username = this.dbConnection.getUsername(currentUserID);
            unreadNotificationsNum = dbConnection.getUnreadNotificationsCountByUser(currentUserID);
        } catch (SQLException e) {
            System.err.println(translation.getTranslation("SQLError"));
            System.exit(1);
        }

        String header = String.format(translation.getTranslation("mainMenuHeader"), username, unreadNotificationsNum);
        System.out.println(header);

        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < mainCommands.length; i++) {
            sb.append(i).append(") ");
            sb.append(translation.getTranslation(mainCommands[i].name())).append('\n');
        }
        System.out.print(sb); // No trailing newline as it was already added on above line
    }

    /**
     * Asks the user which main action he wants to perform
     * @return MainCommand object selected by the user
     */
    public MainCommand parseInput() {
        Integer userSelection = InputManager.inputInteger(translation.getTranslation("userSelection"), false);

        if (userSelection == null || userSelection < 0 || userSelection > mainCommands.length - 1)
            return MainCommand.INVALID;

        return mainCommands[userSelection]; // 0 is INVALID and user available commands start at 1
    }
}
