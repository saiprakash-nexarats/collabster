package com.collabsphere_first.user.entity;

import com.collabsphere_first.role.entity.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "email")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String firstName;

    @Column(nullable = false, length = 50)
    private String lastName;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(length = 255)
    private String password;

    @Column(length = 500)
    private String profilePicture;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthProvider provider;

    @Column(nullable = false)
    private Boolean emailVerified;

    @Column(nullable = false)
    private Boolean accountEnabled;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = true)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;

    @Column(nullable = false)
    @Builder.Default
    private Boolean profileCompleted = false;

    @Column(unique = true, length = 30)
    private String username;

    @Column(length = 100)
    private String country;

    @Column(length = 100)
    private String city;

    @Column(length = 300)
    private String bio;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deactivated = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @Column(nullable = true)
    private LocalDateTime deactivatedAt;

    @Column(nullable = true)
    private LocalDateTime deletedAt;

    @Column(nullable = false)
    @Builder.Default
    private Long authVersion = 0L;

    @PrePersist
    public void onCreate() {

        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (provider == null) {
            provider = AuthProvider.LOCAL;
        }

        if (emailVerified == null) {
            emailVerified = false;
        }

        if (accountEnabled == null) {
            accountEnabled = false;
        }

        if (deactivated == null) {
            deactivated = false;
        }

        if (deleted == null) {
            deleted = false;
        }

        if (authVersion == null) {
            authVersion = 0L;
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Transient
public String getFullName() {
    return firstName + " " + lastName;
}

@Column(length = 500)
private String linkedinUrl;

@Column(length = 500)
private String portfolioUrl;

private Integer yearsOfExperience;
}