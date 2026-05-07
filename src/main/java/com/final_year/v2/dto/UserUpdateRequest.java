package com.final_year.v2.dto;

import com.final_year.v2.constaint.Plan;
import com.final_year.v2.constaint.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserUpdateRequest {
    @Size(min = 3, max = 20)
    private String username;

    @Email
    private String email;

    private Role role;
    private Plan plan;
    private String status;
    private Integer videos;
    private String profilePicture;

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