package edu.dnatools.model;


import com.fasterxml.jackson.annotation.JsonView;

/**
 * Created by luke on 6/9/17.
 */
public class JobInput {

    @JsonView(value = {JsonViews.JobInput.class})
    private String description;

    @JsonView(value = {JsonViews.JobInput.class})
    private String sequence;

    @JsonView(value = {JsonViews.JobInput.class})
    private String forceConstants;
    @JsonView(value = {JsonViews.JobInput.class})
    private String stepParameters;

    @JsonView(value = {JsonViews.JobInput.class})
    private String stepList;

    @JsonView(value = {JsonViews.JobInput.class})
    private int nChains;

    @JsonView(value = {JsonViews.JobInput.class})
    private double rBounds;

    @JsonView(value = {JsonViews.JobInput.class})
    private Boolean useLargeBins = false;

    @JsonView(value = {JsonViews.JobInput.class})
    double gBounds;

    @JsonView(value = {JsonViews.JobInput.class})
    double twBounds;

    @JsonView(value = {JsonViews.JobInput.class})
    private String bc;


    @JsonView(value = {JsonViews.JobInput.class})
    private Long seed;


    @JsonView(value = {JsonViews.JobInput.class})
    private Byte nProteins;

    @JsonView(value = {JsonViews.JobInput.class})
    private Boolean hasProteins = false;

    @JsonView(value = {JsonViews.JobInput.class})
    private Long proteinId = 0L;

    @JsonView(value = {JsonViews.JobInput.class})
    private Double bindingProbability;

    @JsonView(value = {JsonViews.JobInput.class})
    private Boolean hasFixedProteins = false;

    @JsonView(value = {JsonViews.JobInput.class})
    private String fixedProteins = "";

    @JsonView(value = {JsonViews.JobInput.class})
    private Boolean suppressImages = false;

    public double[][][] fc;
    public double[][] tp0;
    public double[][] bounds;

    public String getBc() {
        return bc;
    }

    public void setBc(String bc) {
        this.bc = bc;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStepList() {
        return stepList;
    }

    public void setStepList(String stepList) {
        this.stepList = stepList;
    }

    public String getSequence() {
        return sequence;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    public int getnChains() {
        return nChains;
    }

    public void setnChains(int nChains) {
        this.nChains = nChains;
    }

    public double getrBounds() {
        return rBounds;
    }

    public void setrBounds(double rBounds) {
        this.rBounds = rBounds;
    }

    public double getgBounds() {
        return gBounds;
    }

    public void setgBounds(double gBounds) {
        this.gBounds = gBounds;
    }

    public double getTwBounds() {
        return twBounds;
    }

    public void setTwBounds(double twBounds) {
        this.twBounds = twBounds;
    }

    public String getForceConstants() {
        return forceConstants;
    }

    public void setForceConstants(String forceConstants) {
        this.forceConstants = forceConstants;
    }

    public String getStepParameters() {
        return stepParameters;
    }

    public void setStepParameters(String stepParameters) {
        this.stepParameters = stepParameters;
    }

    public Long getSeed() {
        return seed;
    }

    public void setSeed(Long seed) {
        this.seed = seed;
    }

    public Byte getnProteins() {
        return nProteins;
    }

    public void setnProteins(Byte nProteins) {
        this.nProteins = nProteins;
    }

    public Boolean getHasProteins() {
        return hasProteins;
    }

    public void setHasProteins(Boolean hasProteins) {
        this.hasProteins = hasProteins;
    }

    public Long getProteinId() {
        return proteinId;
    }

    public void setProteinId(Long proteinId) {
        this.proteinId = proteinId;
    }

    public Double getBindingProbability() {
        return bindingProbability;
    }

    public void setBindingProbability(Double bindingProbability) {
        this.bindingProbability = bindingProbability;
    }

    public Boolean getSuppressImages() {
        return suppressImages;
    }

    public void setSuppressImages(Boolean suppressImages) {
        this.suppressImages = suppressImages;
    }

    public Boolean getUseLargeBins() {
        return useLargeBins;
    }

    public void setUseLargeBins(Boolean useLargeBins) {
        this.useLargeBins = useLargeBins;
    }

    public Boolean getHasFixedProteins() {
        return hasFixedProteins;
    }

    public void setHasFixedProteins(Boolean hasFixedProteins) {
        this.hasFixedProteins = hasFixedProteins;
    }

    public String getFixedProteins() {
        return fixedProteins;
    }

    public void setFixedProteins(String fixedProteins) {
        this.fixedProteins = fixedProteins;
    }

}
