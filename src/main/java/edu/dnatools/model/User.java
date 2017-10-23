package edu.dnatools.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;

import javax.persistence.*;

/**
 * Created by luke on 6/21/16.
 */
@Entity
public class User implements IDable<Long> {

    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "email", length = 100, nullable = false, unique = true)
    @JsonView(value = {JsonViews.User.class})
    private String email;

    @Column(name = "first_name", length = 100)
    @JsonView(value = {JsonViews.User.class})
    private String firstName;

    @Column(name = "last_name", length = 100)
    @JsonView(value = {JsonViews.User.class})
    private String lastName;

    @Column(name = "social_id", length = 100, nullable = false)
    @JsonView(value = {JsonViews.User.class})
    private String socialId;

    @Transient
    private String password;

    @JsonIgnore
    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20, nullable = false)
    private Role role;

    @JsonView(value = {JsonViews.User.class})
    @Transient
    private String tokenId;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long newId) {
        id = newId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPassword() {
        return password;
    }

    public Role getRole() {
        return role;
    }

    public String getEmail() {
        return email;
    }

    public String getSocialId() {
        return socialId;
    }

    public String getTokenId() { return tokenId; }

    public void setFirstName(String fn) {
        firstName = fn;
    }

    public void setLastName(String ln) {
        lastName = ln;
    }

    public void setPassword(String pw) {
        password = pw;
    }

    public void setSocialId(String si) {
        socialId = si;
    }

    public void setEmail(String em) {
        email = em;
    }

    public void setRole(Role r) {
        role = r;
    }

    public void setTokenId(String t) { tokenId = t; }

}
