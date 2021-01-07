package edu.dnatools.model;

import com.fasterxml.jackson.annotation.JsonView;

import javax.persistence.*;

/**
 * Created by luke on 6/10/17.
 */
@Entity
public class Protein implements IDable<Long> {

    @Id
    @GeneratedValue
    @JsonView(value = {JsonViews.Protein.class})
    private Long id;

    @JsonView(value = {JsonViews.Protein.class})
    @Column(nullable = false, unique = true)
    private String name;

    @JsonView(value = {JsonViews.Protein.class})
    @Column(nullable = false)
    private Byte number;

    @JsonView(value = {JsonViews.Protein.class})
    @Column(nullable = false)
    private Short stepLength;

    @JsonView(value = {JsonViews.Protein.class})
    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String dats;

    @JsonView(value = {JsonViews.Protein.class})
    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String pdbs;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Byte getNumber() {
        return number;
    }

    public void setNumber(Byte number) {
        this.number = number;
    }

    public Short getStepLength() {
        return stepLength;
    }

    public void setStepLength(Short stepLength) {
        this.stepLength = stepLength;
    }

    public String getDats() {
        return dats;
    }

    public void setDats(String dats) {
        this.dats = dats;
    }

    public String getPdbs() {
        return pdbs;
    }

    public void setPdbs(String pdbs) {
        this.pdbs = pdbs;
    }
}
