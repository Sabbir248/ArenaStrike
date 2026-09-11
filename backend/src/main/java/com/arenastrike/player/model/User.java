package com.arenastrike.player.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected User() {
    }

    public User(String username) {
        this(username, "");
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.createdAt = Instant.now();
    }

    @PrePersist
    void initializeCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getDisplayName() { return username; }
    public String getPassword() { return password; }
    public Instant getCreatedAt() { return createdAt; }
}
