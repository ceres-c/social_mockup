package it.unibs.ing.se.controller;

import it.unibs.ing.se.DMO.JsonTranslator;
import it.unibs.ing.se.model.UserCore;
import it.unibs.ing.se.view.UserProfileView;
import it.unibs.ing.se.view.commands.DashboardCommand;

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
                UserProfileController userProfileController = new UserProfileController(currentUserID);
                UserProfileView userProfileView = new UserProfileView(currentUserID);
                userProfileView.print();
                UserCore updateUserProfile = userProfileView.parseInput();
                userProfileController.perform(updateUserProfile);
                break;
        }
    }
}
