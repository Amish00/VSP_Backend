package com.final_year.v2.model;

import com.final_year.v2.constaint.Plan;
import com.final_year.v2.constaint.Role;
import com.final_year.v2.constaint.UserStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "username"),
        @UniqueConstraint(columnNames = "email")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String username;

    @Column(length = 120)
    private String password;

    @Column(nullable = false, unique = true, length = 50)
    private String email;


    @Enumerated(EnumType.STRING)
    private Role role = Role.VIEWER;

    @Enumerated(EnumType.STRING)
    private Plan plan = Plan.FREE;

    @Column(nullable = false)
    private LocalDate joined;

    @Enumerated(EnumType.STRING)
    private UserStatus status = UserStatus.ACTIVE;

    private Integer videos = 0;

    private String provider;

    @Column(name = "provider_id")
    private String providerId;

    @Column(name = "profile_picture")
    private String profilePicture;

    @Column(name = "subscription_expiry")
    private LocalDateTime subscriptionExpiry;

    // Custom constructor for signup
    public User(String username, String email, String password) {
        this.username = username;
        this.setEmail(email); // triggers lowercase
        this.password = password;
        this.joined = LocalDate.now();
    }

    // Ensure email is always stored in lowercase
    public void setEmail(String email) {
        this.email = email != null ? email.toLowerCase() : null;
    }

    @PrePersist
    public void prePersist() {
        if (joined == null) {
            joined = LocalDate.now();
        }
        if (videos == null) {
            videos = 0;
        }
        if (status == null) {
            status = UserStatus.ACTIVE;
        }
    }

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
    private UserProfile profile;

    public UserProfile getOrCreateProfile() {
        if (profile == null) {
            profile = new UserProfile();
            profile.setUser(this);
        }
        return profile;
    }
}