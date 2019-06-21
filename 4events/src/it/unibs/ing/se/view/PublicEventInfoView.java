package it.unibs.ing.se.view;

import it.unibs.ing.se.view.commands.EventCommand;

import java.util.UUID;

public class PublicEventInfoView extends AbstractEventDetailsView<EventCommand> {
    public PublicEventInfoView (UUID eventID) {
        super(eventID);
    }

    @Override
    public EventCommand parseInput() {
        Character userInput;
        do {
            userInput = InputManager.inputChar(menuTranslation.getTranslation("eventRegistration"), true);
            if (userInput != null && userInput != 'S' && userInput != 'N')
                userInput = null;
        } while (userInput == null);
        if (userInput == 'S')
            return EventCommand.REGISTER;
        return EventCommand.INVALID;
    }
}
