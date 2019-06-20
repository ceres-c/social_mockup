package it.unibs.ing.se.view;

public interface PrintableInterface<T> {

    /**
     * Prints the it.unibs.ing.se.view to the user
     */
    abstract void print();

    /**
     * Returns an appropriate value given the Generic type
     * @return Generic type the class has been initialized with
     */
    abstract T parseInput();
}
