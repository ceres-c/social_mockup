import java.lang.reflect.Field;

public class SoccerGame extends Event {
    public Sex      gender;
    public Integer  ageMin;
    public Integer  ageMax;

    SoccerGame(String catName, String catDescription){
        super(catName, catDescription);
    }

    public void setField(String fieldName, Object content) {
        try {
            Field field = this.getClass().getDeclaredField( fieldName ); // If the field is not owned by this class, then...
            field.set(this, content);
        } catch (NoSuchFieldException e) {
            super.setField(fieldName, content); // ... Search for it in the super class
        } catch (IllegalAccessException e) {
            System.out.println("ALERT: Illegal access on field: " + fieldName);
            e.printStackTrace();
        }
    }
}
