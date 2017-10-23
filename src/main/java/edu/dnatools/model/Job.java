package edu.dnatools.model;

import com.fasterxml.jackson.annotation.JsonView;

import javax.persistence.*;

/**
 * Created by luke on 6/10/17.
 */
@Entity
public class Job implements IDable<Long> {

    @Id
    @GeneratedValue
    @JsonView(value = {JsonViews.Job.class})
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @Column(nullable = false, length = 32, unique = true)
    @JsonView(value = {JsonViews.Job.class})
    private String token;

    @Column(nullable = false)
    @JsonView(value = {JsonViews.Job.class})
    private String description = "";

    @Column(columnDefinition = "LONGTEXT", nullable = false, length = 4096, unique = false)
    @JsonView(value = {JsonViews.Job.class})
    private String sequence;

    @Column(nullable = false, length = 4096, unique = false)
    @JsonView(value = {JsonViews.Job.class})
    private String bounds;

    @Column(columnDefinition = "LONGTEXT", nullable = false, length = 4096, unique = false)
    @JsonView(value = {JsonViews.Job.class})
    private String FC;

    @Column(columnDefinition = "LONGTEXT", nullable = false, length = 4096, unique = false)
    @JsonView(value = {JsonViews.Job.class})
    private String tp0;

    @Column(nullable = false)
    @JsonView(value = {JsonViews.Job.class})
    private Long seed;

    @Column(nullable = false)
    @JsonView(value = {JsonViews.Job.class})
    private Boolean hasProteins = false;

    @Column(nullable = false)
    @JsonView(value = {JsonViews.Job.class})
    private Long proteinId = 0L;

    @Column(nullable = false)
    @JsonView(value = {JsonViews.Job.class})
    private Boolean hasFixedProteins = false;

    @Column(nullable = false)
    @JsonView(value = {JsonViews.Job.class})
    private String fixedProteins = "";


    @Column(nullable = false)
    @JsonView(value = {JsonViews.Job.class})
    private String fixedPositions = "";


    @Column
    @JsonView(value = {JsonViews.Job.class})
    private int nChains;

    @Column
    @JsonView(value = {JsonViews.Job.class})
    private double rBounds;

    @Column
    @JsonView(value = {JsonViews.Job.class})
    private double gBounds;

    @Column
    @JsonView(value = {JsonViews.Job.class})
    private double twBounds;

    @Column
    @JsonView(value = {JsonViews.Job.class})
    private Byte nProteins;

    @Column
    @JsonView(value = {JsonViews.Job.class})
    private Double bindingProbability;

    public String getSequence() {
        return sequence;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBounds() {
        return bounds;
    }

    public void setBounds(String bounds) {
        this.bounds = bounds;
    }

    public String getFC() {
        return FC;
    }

    public void setFC(String FC) {
        this.FC = FC;
    }

    public String getTp0() {
        return tp0;
    }

    public void setTp0(String tp0) {
        this.tp0 = tp0;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public Long getSeed() {
        return seed;
    }

    public void setSeed(Long seed) {
        this.seed = seed;
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

    public Byte getnProteins() {
        return nProteins;
    }

    public void setnProteins(Byte nProteins) {
        this.nProteins = nProteins;
    }

    public Double getBindingProbability() {
        return bindingProbability;
    }

    public void setBindingProbability(Double bindingProbability) {
        this.bindingProbability = bindingProbability;
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

    public String getFixedPositions() {
        return fixedPositions;
    }

    public void setFixedPositions(String fixedPositions) {
        this.fixedPositions = fixedPositions;
    }

    public static Job createFromJobInput(String token, JobInput input) {
        Job submittedJob = new Job();
        submittedJob.setToken(token);
        if (input.getDescription() != null) submittedJob.setDescription(input.getDescription());
        submittedJob.setSequence(input.getSequence());
        submittedJob.setFC(input.getForceConstants());
        submittedJob.setTp0(input.getStepParameters());
        submittedJob.setBounds(input.getBc());
        submittedJob.setSeed(input.getSeed());
        submittedJob.setHasProteins(input.getHasProteins());
        submittedJob.setProteinId(input.getProteinId());
        submittedJob.setnChains(input.getnChains());
        submittedJob.setgBounds(input.getgBounds());
        submittedJob.setTwBounds(input.getTwBounds());
        submittedJob.setrBounds(input.getrBounds());
        submittedJob.setHasFixedProteins(input.getHasFixedProteins());

        submittedJob.setBindingProbability(input.getBindingProbability());
        submittedJob.setnProteins(input.getHasProteins() ? (byte)1 : (byte)0);
        return submittedJob;
    }


}
