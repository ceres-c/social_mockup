package it.unibs.ing.se.controller;

import it.unibs.ing.se.DMO.JsonTranslator;
import it.unibs.ing.se.model.UserCore;
import it.unibs.ing.se.view.*;
import it.unibs.ing.se.view.commands.DashboardCommand;
import it.unibs.ing.se.view.commands.EventCommand;

import java.util.ArrayList;
import java.util.UUID;

public class DashboardController implements ControllerInterface<DashboardCommand>  {
    private JsonTranslator menuTranslation;
    private UUID currentUserID;

    DashboardController(UUID currentUserID) {
        this.menuTranslation = new JsonTranslator(JsonTranslator.MENU_JSON_PATH);
        this.currentUserID = currentUserID;
    }

    @Override
    public void perform(DashboardCommand selection) {
        switch (selection) {
            case INVALID:
                System.err.println(menuTranslation.getTranslation("invalidUserSelection"));
                break;
            case USER_PROFILE:
                UserProfileView userProfileView = new UserProfileView(currentUserID);
                userProfileView.print();
                UserCore updateUserProfile = userProfileView.parseInput();
                UserProfileController userProfileController = new UserProfileController(currentUserID);
                userProfileController.perform(updateUserProfile);
                break;
            case PERSONAL_NOTIFICATIONS:
                PersonalNotificationView personalNotificationView = new PersonalNotificationView(currentUserID);
                personalNotificationView.print();
                ArrayList<UUID> readNotifications = personalNotificationView.parseInput();
                PersonalNotificationController personalNotificationController = new PersonalNotificationController();
                personalNotificationController.perform(readNotifications);
                break;
            case CREATED_EVENTS:
                CreatedEventsView createdEventsView = new CreatedEventsView(currentUserID);
                createdEventsView.createWorkingSet(); // Fetch all the events a user has created
                createdEventsView.print();
                UUID selectedEvent = createdEventsView.parseInput(); // UUID of the Event selected by the user
                if (selectedEvent == null) // In case the user did not select any event
                    break;

                CreatedEventInfoView eventInfoView = new CreatedEventInfoView(selectedEvent);
                eventInfoView.print();
                EventCommand userCommand = eventInfoView.parseInput();
                CreatedEventsController createdEventsController = new CreatedEventsController(selectedEvent);
                createdEventsController.perform(userCommand);
                break;
            case REGISTERED_EVENTS:
                RegisteredEventsView registeredEventsView = new RegisteredEventsView(currentUserID);
                registeredEventsView.createWorkingSet(); // Fetch all the events a user has registered to
                registeredEventsView.print();
                // TODO controller eventi registrati
        }
    }
}
