package edu.dnatools.model;

import com.fasterxml.jackson.annotation.JsonView;
import com.google.api.client.json.Json;

/**
 * Created by luke on 6/10/17.
 */
public class Results {


    @JsonView(value = {JsonViews.Results.class})
    public String Jfactor;

    @JsonView(value = {JsonViews.Results.class})
    public String twistHistogram;

    @JsonView(value = {JsonViews.Results.class})
    public String writheHistogram;

    @JsonView(value = {JsonViews.Results.class})
    public String linkHistogram;

    @JsonView(value = {JsonViews.Results.class})
    public String positionHistogram;

    @JsonView(value = {JsonViews.Results.class})
    public String numberHistogram;

    @JsonView(value = {JsonViews.Results.class})
    public String distanceHistogram;

    @JsonView(value = {JsonViews.Results.class})
    public String rgHistogram;

}
