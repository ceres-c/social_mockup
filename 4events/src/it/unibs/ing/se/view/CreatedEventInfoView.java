package it.unibs.ing.se.view;

import it.unibs.ing.se.view.commands.EventCommand;

import java.util.UUID;

public class CreatedEventInfoView extends AbstractEventDetailsView<EventCommand> {
    public CreatedEventInfoView (UUID eventID) {
        super(eventID);
    }

    @Override
    public EventCommand parseInput() {
        Character userInput;
        do {
            userInput = InputManager.inputChar(menuTranslation.getTranslation("eventPublication"), true);
            if (userInput != null && userInput != 'S' && userInput != 'N')
                userInput = null;
        } while (userInput == null);
        if (userInput == 'S')
            return EventCommand.PUBLISH;
        do {
            userInput = InputManager.inputChar(menuTranslation.getTranslation("eventWithdraw"), true);
            if (userInput != null && userInput != 'S' && userInput != 'N')
                userInput = null;
        } while (userInput == null);
        if (userInput == 'S')
            return EventCommand.WITHDRAW;
        return EventCommand.INVALID;
    }
}
