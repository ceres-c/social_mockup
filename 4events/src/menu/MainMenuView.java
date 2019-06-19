package menu;

import DMO.Connector;
import DMO.JsonTranslator;
import menu.commands.MainCommand;
import model.User;

import java.sql.SQLException;

public class MainMenuView implements PrintableInterface<MainCommand> {
    JsonTranslator menuTranslation;
    Connector dbConnection;
    User currentUser;
    MainCommand[] mainCommands = MainCommand.values();

    /**
     * @param translation JsonTranslator object with menu entries
     * @param dbConnection Connector object already initialized
     * @param currentUser User object of current user
     */
    public MainMenuView(JsonTranslator translation, Connector dbConnection, User currentUser) {
        this.menuTranslation = translation;
        this.dbConnection = dbConnection;
        this.currentUser = currentUser;
    }

    /**
     * Prints available options (took from MainCommands)
     */
    public void print() {
        String username = this.currentUser.getUsername();
        int unreadNotificationsNum = 0;
        try {
            unreadNotificationsNum = dbConnection.getUnreadNotificationsCountByUser(currentUser);
        } catch (SQLException e) {
            System.err.println(menuTranslation.getTranslation("SQLError"));
            System.exit(1);
        }

        System.out.println(menuTranslation.getTranslation("welcome"));

        String header = String.format(menuTranslation.getTranslation("mainMenuHeader"), username, unreadNotificationsNum);
        System.out.println(header);

        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < mainCommands.length; i++) {
            sb.append(i).append(") ");
            sb.append(menuTranslation.getTranslation(mainCommands[i].name())).append('\n');
        }
        System.out.print(sb); // No trailing newline as it was already added on above line
    }

    /**
     * Asks the user which main action he wants to perform
     * @return MainCommand object selected by the user
     */
    public MainCommand parseInput() {
        Integer userSelection = InputManager.inputInteger(menuTranslation.getTranslation("userSelection"), false);

        MainCommand[] mainCommands = MainCommand.values();

        if (userSelection == null || userSelection < 0 || userSelection >= mainCommands.length)
            return MainCommand.INVALID;

        return mainCommands[userSelection]; // 0 is INVALID and user available commands start at 1
    }
}
