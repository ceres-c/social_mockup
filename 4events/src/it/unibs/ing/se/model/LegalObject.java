package it.unibs.ing.se.model;

import java.time.LocalDateTime;

public interface LegalObject {
    /**
     * Used to check if a newly created event is logically valid
     * @param currentDate
     */
    boolean isLegal(LocalDateTime currentDate);
}
