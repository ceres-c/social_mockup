package it.unibs.ing.se.controller;

import it.unibs.ing.se.DMO.JsonTranslator;
import it.unibs.ing.se.view.DashboardView;
import it.unibs.ing.se.view.PublicEventInfoView;
import it.unibs.ing.se.view.PublicEventsView;
import it.unibs.ing.se.view.commands.DashboardCommand;
import it.unibs.ing.se.view.commands.EventCommand;
import it.unibs.ing.se.view.commands.MainCommand;

import java.util.UUID;

public class MainMenuController implements ControllerInterface<MainCommand> {
    private JsonTranslator menuTranslation;
    private LoginHelper loginHelper;
    private UUID currentUserID;

    public MainMenuController() {
        this.menuTranslation = new JsonTranslator(JsonTranslator.MENU_JSON_PATH);
        this.loginHelper = new LoginHelper();
    }

    /**
     * @return UUID of the currently logged in User
     * @throws IllegalStateException if loginAndSet hasn't been performed yet
     */
    public UUID getCurrentUserID() throws IllegalStateException {
        return currentUserID;
    }

    public void loginAndSet() {
        while ((currentUserID = loginHelper.login()) == null);
    }

    @Override
    public void perform(MainCommand selection) {
        switch (selection) {
            case INVALID:
                System.err.println(menuTranslation.getTranslation("invalidUserSelection"));
                break;
            case DASHBOARD:
                DashboardView dashboardView = new DashboardView();
                dashboardView.print();
                DashboardCommand userSelection = dashboardView.parseInput();
                DashboardController dashController = new DashboardController (currentUserID);
                dashController.perform(userSelection);
                break;
            case PUBLIC_EVENTS_LIST:
                PublicEventsView publicEventsView = new PublicEventsView();
                publicEventsView.createWorkingSet();
                publicEventsView.print();
                UUID selectedEvent = publicEventsView.parseInput();
                if (selectedEvent == null) // In case the user did not select any event
                    break;

                PublicEventInfoView publicEventInfoView = new PublicEventInfoView(selectedEvent);
                publicEventInfoView.print();
                EventCommand userCommand = publicEventInfoView.parseInput();
                PublicEventsController publicEventsController = new PublicEventsController(selectedEvent, currentUserID);
                publicEventsController.perform(userCommand);
                break;
        }
    }
}
