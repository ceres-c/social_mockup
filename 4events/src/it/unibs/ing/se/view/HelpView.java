package it.unibs.ing.se.view;

import it.unibs.ing.se.DMO.Connector;
import it.unibs.ing.se.DMO.JsonTranslator;
import it.unibs.ing.se.model.Event;
import it.unibs.ing.se.model.EventFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class HelpView {
    JsonTranslator eventTranslation;
    ArrayList<String> categories;

    public HelpView() {
        this.eventTranslation = new JsonTranslator(JsonTranslator.EVENT_JSON_PATH);
        try {
            categories = Connector.getInstance().getCategories();
        } catch (SQLException e) {
            System.err.println(eventTranslation.getTranslation("SQLError"));
            System.exit(1);
        }
    }

    public void print() {
        System.out.println(eventTranslation.getTranslation("categoryList"));

        EventFactory eFactory = new EventFactory();
        Event event;
        String internalCatName;
        int maxLength;

        for (int i = 0; i < categories.size(); i++) {
            internalCatName = categories.get(i);
            maxLength = 0;

            System.out.println(eventTranslation.getName(internalCatName) + " - " + eventTranslation.getDescr(internalCatName));

            event = eFactory.createEvent(internalCatName);

            LinkedHashMap<String, Class<?>> eventFieldsMap = event.getAttributesWithType();

            Iterator iterator = eventFieldsMap.entrySet().iterator(); // Get an iterator for our map

            while(iterator.hasNext()) {
                Map.Entry entry = (Map.Entry)iterator.next(); // Casts the iterated item to a Map Entry to use it as such

                int length = eventTranslation.getName((String)entry.getKey()).length();
                if (length > maxLength)
                    maxLength = length; // ...find the longest
            }

            maxLength += 3; // Add some more char to allow spacing between the longest name and its description
            iterator = eventFieldsMap.entrySet().iterator(); // Reset to first element
            StringBuffer outputBuffer = new StringBuffer();

            while(iterator.hasNext()) {
                Map.Entry entry = (Map.Entry)iterator.next(); // Casts the iterated item to a Map Entry to use it as such
                String field = (String)entry.getKey();

                outputBuffer.append("  ");
                outputBuffer.append(eventTranslation.getName(field));
                outputBuffer.append(':');
                for (int in = 0; in < (maxLength - eventTranslation.getName(field).length()); in++) { // Wonderful onelined math...
                    outputBuffer.append(" "); // ...for spacing purposes
                }
                outputBuffer.append(eventTranslation.getDescr(field)).append('\n');
            }

            System.out.println(outputBuffer);
        }
    }
}
