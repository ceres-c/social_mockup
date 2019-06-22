package it.unibs.ing.se.controller;

import it.unibs.ing.se.DMO.Connector;
import it.unibs.ing.se.DMO.JsonTranslator;
import it.unibs.ing.se.controller.helpers.EventHelper;
import it.unibs.ing.se.controller.helpers.NotificationHelper;
import it.unibs.ing.se.model.Event;
import it.unibs.ing.se.model.EventFactory;
import it.unibs.ing.se.view.NewEventInvitesView;
import it.unibs.ing.se.view.NewEventPublishView;
import it.unibs.ing.se.view.inputwrappers.EventInput;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class NewEventController implements ControllerInterface<EventInput> {
    private UUID userID;
    private JsonTranslator translation;
    private Connector dbConnection;

    NewEventController (UUID userID) {
        this.translation = JsonTranslator.getInstance();
        dbConnection = Connector.getInstance();
        this.userID = userID;
    }

    @Override
    public void perform(EventInput selection) {
        EventFactory eFactory = new EventFactory();
        Event newEvent = eFactory.createEvent(UUID.randomUUID(), userID, selection.getEventType());
        LinkedHashMap<String, Object> inputAttributes = selection.getNonNullAttributesWithValue();
        Iterator iterator = inputAttributes.entrySet().iterator(); // Get an iterator for our map

        while(iterator.hasNext()) {
            Map.Entry entry = (Map.Entry)iterator.next();
            newEvent.setAttribute((String) entry.getKey(), entry.getValue());
        }
        try {
            newEvent.isLegal(LocalDateTime.now());
        } catch (IllegalStateException e) {
            System.err.println(translation.getTranslation("eventNotLegal"));
            System.err.println(e.getMessage());
            return;
        }

        newEvent.setAttribute("participantsMax",
                newEvent.participantsMin +
                        (newEvent.participantsSurplus == null ? 0 : newEvent.participantsSurplus));

        if (newEvent.deregistrationDeadline == null) // As specified by client request, if deregistration deadline wasn't set...
            newEvent.setAttribute("deregistrationDeadline", newEvent.registrationDeadline); // it defaults to registration deadline

        newEvent.updateState(LocalDateTime.now()); // UNKNOWN -> VALID
        try {
            dbConnection.insertEvent(newEvent);
        } catch (SQLException e) {
            System.err.println(translation.getTranslation("SQLError"));
            System.exit(1);
        }

        EventHelper eHelper = new EventHelper();
        eHelper.register(newEvent.getEventID(), userID);

        NewEventPublishView newEventPublishView = new NewEventPublishView();
        boolean publicationNeeded = newEventPublishView.parseInput();
        if (publicationNeeded) {
            eHelper.publish(newEvent.getEventID(), true);
            eHelper.updateStatus(newEvent.getEventID());

            NewEventInvitesView newEventInvitesView = new NewEventInvitesView();
            boolean invitesNeeded = newEventInvitesView.parseInput();
            if (invitesNeeded) {
                NotificationHelper notHelper = new NotificationHelper(newEvent.getEventID());
                notHelper.sendInvites();
            }
        }
    }
}
