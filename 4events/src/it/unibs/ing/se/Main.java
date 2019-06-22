package it.unibs.ing.se;

import it.unibs.ing.se.DMO.Connector;
import it.unibs.ing.se.DMO.JsonConfigReader;
import it.unibs.ing.se.DMO.JsonTranslator;
import it.unibs.ing.se.controller.MainMenuController;
import it.unibs.ing.se.controller.helpers.EventHelper;
import it.unibs.ing.se.view.*;
import it.unibs.ing.se.view.commands.MainCommand;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;

public class Main {
    private static final String CONFIG_JSON_PATH = "config.json";

    public static void main(String[] args) {
        Path configJsonPath = Paths.get(CONFIG_JSON_PATH);
        JsonConfigReader config = new JsonConfigReader(configJsonPath.toString());

        JsonTranslator translation = JsonTranslator.getInstance();

        try { // Initializes the Connector for future usage
            Connector.getInstance(config.getDBURL(), config.getDBUser(), config.getDBPassword());
        } catch (SQLException e) {
            System.err.println(translation.getTranslation("SQLError"));
            System.exit(1);
        }

        EventHelper eHelper = new EventHelper();
        eHelper.updateAllEvents();

        System.out.println(translation.getTranslation("welcome"));

        MainMenuController mainController = new MainMenuController();
        mainController.loginAndSet();

        UUID currentUserID = mainController.getCurrentUserID();
        MainMenuView mainView = new MainMenuView(currentUserID);
        MainCommand userSelection;

        while (true) {
            mainView.print();
            userSelection = mainView.parseInput();
            mainController.perform(userSelection);
        }
    }
}