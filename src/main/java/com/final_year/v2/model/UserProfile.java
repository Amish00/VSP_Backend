// src/main/java/com/final_year/v2/model/UserProfile.java
package com.final_year.v2.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "user_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    private String fullName;
    private String phone;
    private LocalDate dob;
    private String gender;
    private String country;
    private String bio;
    private String facebook;
    private String twitter;
    private String instagram;
    private String bannerUrl;
}