package edu.dnatools.model;

import com.fasterxml.jackson.annotation.JsonView;

import javax.persistence.*;

/**
 * Created by luke on 6/13/17.
 */
@Entity
public class BoundaryCondition implements IDable<Long> {

    @Id
    @GeneratedValue
    @JsonView(value = {JsonViews.BoundaryCondition.class})
    private Long id;

    @Column(nullable = false, unique = true)
    @JsonView(value = {JsonViews.BoundaryCondition.class})
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @JsonView(value = {JsonViews.BoundaryCondition.class})
    User user;

    @Column(nullable = false)
    @JsonView(value = {JsonViews.BoundaryCondition.class})
    private Double tilt;
    @Column(nullable = false)
    @JsonView(value = {JsonViews.BoundaryCondition.class})
    private Double roll;
    @Column(nullable = false)
    @JsonView(value = {JsonViews.BoundaryCondition.class})
    private Double twist;
    @Column(nullable = false)
    @JsonView(value = {JsonViews.BoundaryCondition.class})
    private Double shift;
    @Column(nullable = false)
    @JsonView(value = {JsonViews.BoundaryCondition.class})
    private Double slide;
    @Column(nullable = false)
    @JsonView(value = {JsonViews.BoundaryCondition.class})
    private Double rise;


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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getTilt() {
        return tilt;
    }

    public void setTilt(Double tilt) {
        this.tilt = tilt;
    }

    public Double getRoll() {
        return roll;
    }

    public void setRoll(Double roll) {
        this.roll = roll;
    }

    public Double getTwist() {
        return twist;
    }

    public void setTwist(Double twist) {
        this.twist = twist;
    }

    public Double getShift() {
        return shift;
    }

    public void setShift(Double shift) {
        this.shift = shift;
    }

    public Double getSlide() {
        return slide;
    }

    public void setSlide(Double slide) {
        this.slide = slide;
    }

    public Double getRise() {
        return rise;
    }

    public void setRise(Double rise) {
        this.rise = rise;
    }
}
