package com.golmok.core.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String nickname;

    @Column(nullable = false, length = 11)
    private String phoneNumber;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant deletedAt;

    protected User() {
    }

    public static User create(String nickname, String phoneNumber, Instant createdAt) {
        User user = new User();
        user.nickname = nickname;
        user.phoneNumber = phoneNumber;
        user.createdAt = createdAt;
        return user;
    }

    public Long getId() {
        return id;
    }

    public String getNickname() {
        return nickname;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
