package it.unibs.ing.se.model;

import java.time.LocalDateTime;

public interface LegalObject {
    /**
     * Used to check if a newly created event is logically valid
     * @param currentDate LocalDateTime object with the date to check against if status has to be updated or not
     */
    boolean isLegal(LocalDateTime currentDate);
}
