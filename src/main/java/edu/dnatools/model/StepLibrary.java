package edu.dnatools.model;

import com.fasterxml.jackson.annotation.JsonView;

/**
 * Created by luke on 8/3/17.
 */
public class StepLibrary {

    @JsonView(value = {JsonViews.StepLibrary.class})
    String[] contexts;

    @JsonView(value = {JsonViews.StepLibrary.class})
    Double[][] steps;

    public String[] getContexts() {
        return contexts;
    }

    public void setContexts(String[] contexts) {
        this.contexts = contexts;
    }

    public Double[][] getSteps() {
        return steps;
    }

    public void setSteps(Double[][] steps) {
        this.steps = steps;
    }
}
