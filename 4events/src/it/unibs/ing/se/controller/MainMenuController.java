package it.unibs.ing.se.controller;

import it.unibs.ing.se.DMO.Connector;
import it.unibs.ing.se.DMO.JsonTranslator;
import it.unibs.ing.se.helpers.LoginHelper;
import it.unibs.ing.se.view.*;
import it.unibs.ing.se.view.commands.DashboardCommand;
import it.unibs.ing.se.view.commands.EventCommand;
import it.unibs.ing.se.view.commands.MainCommand;
import it.unibs.ing.se.view.inputwrappers.EventInput;

import java.util.UUID;

public class MainMenuController implements ControllerInterface<MainCommand> {
    private JsonTranslator translation;
    private LoginHelper loginHelper;
    private UUID currentUserID;

    public MainMenuController() {
        this.translation = JsonTranslator.getInstance();
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
                System.err.println(translation.getTranslation("invalidUserSelection"));
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
            case NEW_EVENT:
                NewEventView newEventView = new NewEventView();
                newEventView.print();
                EventInput userInput = newEventView.parseInput();
                NewEventController newEventController = new NewEventController(currentUserID);
                newEventController.perform(userInput);
                break;
            case HELP:
                HelpView helpView = new HelpView();
                helpView.print();
                break;
            case QUIT:
                Connector.getInstance().closeDb();
                System.out.println(translation.getTranslation("exit"));
                System.exit(0);
                break;
        }
    }
}
