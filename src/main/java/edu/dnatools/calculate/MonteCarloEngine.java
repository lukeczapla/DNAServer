package edu.dnatools.calculate;

/**
 * Created by luke on 7/1/17.
 */
public interface MonteCarloEngine {
    void move();
    boolean test();
    void accept();
    void reject();
}
