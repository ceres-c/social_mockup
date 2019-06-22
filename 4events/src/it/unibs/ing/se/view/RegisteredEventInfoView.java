package it.unibs.ing.se.view;

import it.unibs.ing.se.view.commands.EventCommand;

import java.util.UUID;

public class RegisteredEventInfoView extends AbstractEventDetailsView<EventCommand> {
    public RegisteredEventInfoView (UUID eventID) {
        super(eventID);
    }

    @Override
    public EventCommand parseInput() {
        Character userInput;
        do {
            userInput = InputManager.inputChar(translation.getTranslation("eventDeregister"), true);
            if (userInput != null && userInput != 'S' && userInput != 'N')
                userInput = null;
        } while (userInput == null);
        if (userInput == 'S')
            return EventCommand.DEREGISTER;
        return EventCommand.INVALID;
    }
}
