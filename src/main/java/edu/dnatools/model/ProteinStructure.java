package edu.dnatools.model;

import com.fasterxml.jackson.annotation.JsonView;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * Created by luke on 6/17/17.
 */
@Entity
public class ProteinStructure implements IDable<Long> {

    @Id
    @GeneratedValue
    @JsonView(value = {JsonViews.ProteinStructure.class})
    private Long id;

    @Column(nullable = false)
    @JsonView(value = {JsonViews.ProteinStructure.class})
    private String name;

    @Column
    @JsonView(value = {JsonViews.ProteinStructure.class})
    private String description;

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    @JsonView(value = {JsonViews.ProteinStructure.class})
    private String PDBtext;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getPDBtext() {
        return PDBtext;
    }

    public void setPDBtext(String PDBtext) {
        this.PDBtext = PDBtext;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
