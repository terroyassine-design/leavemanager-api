package com.leavemanager.api.user;

import java.util.Locale;

import com.leavemanager.api.common.ApiException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    public static final int DEFAULT_LEAVE_BALANCE = 25;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /** Remaining paid leave days. */
    @Column(nullable = false)
    private int leaveBalance;

    /** Shared demo accounts: their profile is read-only so visitors cannot lock others out. */
    @Column(nullable = false)
    private boolean demo;

    /** Incremented on each password change to revoke previously issued tokens. */
    @Column(nullable = false)
    private int sessionVersion;

    protected User() {
        // required by JPA
    }

    public User(String fullName, String email, String encodedPassword, Role role) {
        this.fullName = fullName;
        this.email = email;
        this.password = encodedPassword;
        this.role = role;
        this.leaveBalance = DEFAULT_LEAVE_BALANCE;
    }

    public void debitLeaveBalance(int days) {
        if (days > leaveBalance) {
            throw ApiException.conflict("Solde de congés insuffisant");
        }
        leaveBalance -= days;
    }

    public void updateProfile(String fullName, String email) {
        ensureEditable();
        this.fullName = fullName;
        this.email = email;
    }

    public void changePassword(String encodedPassword) {
        ensureEditable();
        this.password = encodedPassword;
        this.sessionVersion++;
    }

    public void markAsDemo() {
        this.demo = true;
    }

    private void ensureEditable() {
        if (demo) {
            throw ApiException.forbidden("Le profil d'un compte de démo ne peut pas être modifié. Créez votre propre compte pour tester cette fonctionnalité.");
        }
    }

    /** Emails are stored trimmed and lower-cased so that login is case-insensitive. */
    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public Role getRole() {
        return role;
    }

    public int getLeaveBalance() {
        return leaveBalance;
    }

    public boolean isDemo() {
        return demo;
    }

    public int getSessionVersion() {
        return sessionVersion;
    }
}
