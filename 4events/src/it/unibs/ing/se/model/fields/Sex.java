package it.unibs.ing.se.model.fields;

public class Sex {
    public static final Character MALE = 'M';
    public static final Character FEMALE = 'F';

    private char sex;

    public Sex (Character sex) throws IllegalStateException {
        if (sex != MALE || sex != FEMALE)
            throw new IllegalArgumentException("Illegal input " + sex);
        this.sex = sex;
    }

    public Sex (String sex) {
        if (sex.charAt(0) != MALE || sex.charAt(0) !=FEMALE)
            throw new IllegalArgumentException("Illegal input " + sex.charAt(0));
        this.sex = sex.charAt(0);
    }

    public char getSex() {
        return sex;
    }

    public boolean equals(Sex other) { return this.sex == other.getSex(); }

    @Override
    public String toString () {
        return Character.toString(sex);
    }
}
