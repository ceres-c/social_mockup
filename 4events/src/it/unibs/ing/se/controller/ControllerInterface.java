package it.unibs.ing.se.controller;

public interface ControllerInterface<K> {

    /**
     * Performs an action basing on a object of type K
     * @param selection The object to switch on
     */
    void perform(K selection);
}
