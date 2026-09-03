package com.workforce.vms.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/** Application user (used for /login + Datadog usr.id attribution). */
@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(name = "username")
    private String username;

    /** DEMO-VULN: Weak crypto — passwords are stored as unsalted MD5 (see PasswordHasher). */
    @Column(name = "password_md5")
    private String passwordMd5;

    @Column(name = "email")
    private String email;

    @Column(name = "role")
    private String role;

    public User() { }

    public User(String username, String passwordMd5, String email, String role) {
        this.username = username;
        this.passwordMd5 = passwordMd5;
        this.email = email;
        this.role = role;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordMd5() { return passwordMd5; }
    public void setPasswordMd5(String passwordMd5) { this.passwordMd5 = passwordMd5; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
