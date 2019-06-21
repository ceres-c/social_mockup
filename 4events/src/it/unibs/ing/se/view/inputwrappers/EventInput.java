package it.unibs.ing.se.view.inputwrappers;

import it.unibs.ing.se.model.ReflectionInterface;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * This abstract class and its derived classes are used as data wrappers to pass raw user's input data
 * from the view to the controller. There it will be sanitized and translated into model objects
 */
abstract public class EventInput implements ReflectionInterface {
    private String          eventType; // As present in DB category list: ie. "soccer_game"

    public  String          title;
    public  Integer         participantsMin;
    public  Integer         participantsSurplus;
    public  LocalDateTime   registrationDeadline;
    public  String          location;
    public  LocalDateTime   startDate;
    public  Duration        duration;
    public  LocalDateTime   deregistrationDeadline;
    public  Double          cost;
    public  String          inQuota;
    public  LocalDateTime   endDate;
    public  String          notes;

    private final String[] mandatoryFields = {"participantsMin", "registrationDeadline", "location", "startDate", "cost"};

    EventInput(String eventType) {
        this.eventType = eventType;
    }

    public String getEventType() {
        return eventType;
    }

    /**
     * A method to set a field to a given object passed from the caller
     */
    public void setAttribute(String fieldName, Object content) {
        try {
            Field field = this.getClass().getSuperclass().getDeclaredField( fieldName ); // Need to get the super class as this is an ancestor of real instance classes
            field.set(this, content);
        } catch (NoSuchFieldException e) {
            System.out.println("ALERT: Missing field: " + fieldName);
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            System.out.println("ALERT: Illegal access on field: " + fieldName);
            e.printStackTrace();
        }
    }

    /**
     * A method to check if a field is mandatory or optional
     */
    public boolean isOptional(String fieldName) {
        for (String field:mandatoryFields)
            if (fieldName.equals(field)) return false;
        return true;
    }

    @Override
    public String toString() {
        return "EventInput{" +
                "eventType='" + eventType + '\'' +
                ", title='" + title + '\'' +
                ", participantsMin=" + participantsMin +
                ", participantsSurplus=" + participantsSurplus +
                ", registrationDeadline=" + registrationDeadline +
                ", location='" + location + '\'' +
                ", startDate=" + startDate +
                ", duration=" + duration +
                ", deregistrationDeadline=" + deregistrationDeadline +
                ", cost=" + cost +
                ", inQuota='" + inQuota + '\'' +
                ", endDate=" + endDate +
                ", notes='" + notes + '\'' +
                ", mandatoryFields=" + Arrays.toString(mandatoryFields) +
                '}';
    }
}
