package edu.dnatools.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonView;
import com.google.api.client.json.Json;

import javax.persistence.Entity;
import java.util.Date;

/**
 * Created by luke on 6/13/17.
 */
public class PDBinput {

    @JsonView(value = {JsonViews.PDBinput.class})
    private String pdbs;

    @JsonView(value = {JsonViews.PDBinput.class})
    private String pdbList;

    @JsonView(value = {JsonViews.PDBinput.class})
    private String description;

    @JsonView(value = {JsonViews.PDBinput.class})
    private Boolean RNA = false;

    @JsonView(value = {JsonViews.PDBinput.class})
    private Boolean nonredundant = false;

    @JsonView(value = {JsonViews.PDBinput.class})
    @JsonFormat(pattern="yyyy-MM-dd")
    private Date beforeDate = new Date();

    @JsonView(value = {JsonViews.PDBinput.class})
    @JsonFormat(pattern="yyyy-MM-dd")
    private Date afterDate = null;

    @JsonView(value = {JsonViews.PDBinput.class})
    private Boolean cullStandard;

    @JsonView(value = {JsonViews.PDBinput.class})
    private Boolean cullEigen;


    public String getPdbs() {
        return pdbs;
    }

    public void setPdbs(String pdbs) {
        this.pdbs = pdbs;
    }

    public String getPdbList() {
        return pdbList;
    }

    public void setPdbList(String pdbList) {
        this.pdbList = pdbList;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean isRNA() {
        return RNA;
    }

    public void setRNA(Boolean RNA) {
        this.RNA = RNA;
    }

    public Boolean isNonredundant() {
        return nonredundant;
    }

    public void setNonredundant(Boolean nonredundant) {
        this.nonredundant = nonredundant;
    }

    public Date getBeforeDate() {
        return beforeDate;
    }

    public void setBeforeDate(Date beforeDate) {
        this.beforeDate = beforeDate;
    }

    public Date getAfterDate() {
        return afterDate;
    }

    public void setAfterDate(Date afterDate) {
        this.afterDate = afterDate;
    }

    public Boolean getCullStandard() {
        return cullStandard;
    }

    public void setCullStandard(Boolean cullStandard) {
        this.cullStandard = cullStandard;
    }

    public Boolean getCullEigen() {
        return cullEigen;
    }

    public void setCullEigen(Boolean cullEigen) {
        this.cullEigen = cullEigen;
    }
}
