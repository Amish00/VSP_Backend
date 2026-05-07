package com.final_year.v2.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.final_year.v2.constaint.Plan;
import com.final_year.v2.constaint.Role;
import com.final_year.v2.constaint.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private Role role;
    private Plan plan;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate joined;
    private String status;
    private Integer videos;
    private String profilePicture;

    private LocalDateTime subscriptionExpiry;

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