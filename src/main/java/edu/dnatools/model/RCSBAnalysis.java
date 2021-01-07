package edu.dnatools.model;

import com.fasterxml.jackson.annotation.JsonView;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by luke on 7/22/17.
 */
@Entity
public class RCSBAnalysis implements IDable<Long> {

    @Id
    @GeneratedValue
    @JsonView(value = {JsonViews.RCSBAnalysis.class})
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @JsonView(value = {JsonViews.RCSBAnalysis.class})
    @Column
    private String code;

    @JsonView(value = {JsonViews.RCSBAnalysis.class})
    @Column(columnDefinition = "LONGTEXT")
    private String pdbList;

    @JsonView(value = {JsonViews.RCSBAnalysis.class})
    @Column
    private String description;

    @JsonView(value = {JsonViews.RCSBAnalysis.class})
    @Column
    private Date beforeDate;

    @JsonView(value = {JsonViews.RCSBAnalysis.class})
    @Column
    private Date afterDate;

    @JsonView(value = {JsonViews.RCSBAnalysis.class})
    @Column
    private Boolean redundant;

    @JsonView(value = {JsonViews.RCSBAnalysis.class})
    @Column
    private Boolean RNA = false;

    @JsonView(value = {JsonViews.RCSBAnalysis.class})
    @Column
    private Boolean done = false;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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

    public Boolean isRedundant() {
        return redundant;
    }

    public void setRedundant(Boolean redundant) {
        this.redundant = redundant;
    }

    public Boolean isRNA() {
        return RNA;
    }

    public void setRNA(Boolean RNA) {
        this.RNA = RNA;
    }

    public Boolean isDone() {
        return done;
    }

    public void setDone(Boolean done) {
        this.done = done;
    }

    public static RCSBAnalysis createFromPDBinput(PDBinput p) {
        RCSBAnalysis entry = new RCSBAnalysis();
        entry.pdbList = p.getPdbList();
        entry.description = p.getDescription();
        entry.redundant = p.isNonredundant();
        entry.RNA = p.isRNA();
        entry.beforeDate = p.getBeforeDate();
        entry.afterDate = p.getAfterDate();
        return entry;
    }

}
