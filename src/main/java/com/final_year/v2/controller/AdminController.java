package com.final_year.v2.controller;

import com.final_year.v2.constaint.Plan;
import com.final_year.v2.constaint.Role;
import com.final_year.v2.dto.MessageResponse;
import com.final_year.v2.model.User;
import com.final_year.v2.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @PutMapping("/users/{id}/role")
    public ResponseEntity<?> updateUserRole(@PathVariable Long id, @RequestParam Role role) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        user.setRole(role);
        userRepository.save(user);
        return ResponseEntity.ok(new MessageResponse("Role updated to " + role));
    }

    @PutMapping("/users/{id}/plan")
    public ResponseEntity<?> updateUserPlan(@PathVariable Long id, @RequestParam Plan plan) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        user.setPlan(plan);
        userRepository.save(user);
        return ResponseEntity.ok(new MessageResponse("Plan updated to " + plan));
    }
}