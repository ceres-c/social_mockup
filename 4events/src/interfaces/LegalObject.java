package interfaces;

import java.util.Calendar;

public interface LegalObject {
    /**
     * Used to check if a newly created event is logically valid
     */
    boolean isLegal();
}
