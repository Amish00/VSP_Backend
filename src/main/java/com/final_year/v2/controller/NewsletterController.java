package com.final_year.v2.controller;

import com.final_year.v2.model.NewsletterSubscriber;
import com.final_year.v2.repository.NewsletterSubscriberRepository;
import com.final_year.v2.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/newsletter")
@RequiredArgsConstructor
public class NewsletterController {

    private final NewsletterSubscriberRepository repository;
    private final EmailService emailService;

    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribe(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is required"));
        }

        if (repository.existsByEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email already subscribed"));
        }

        NewsletterSubscriber subscriber = NewsletterSubscriber.builder()
                .email(email)
                .subscribedAt(LocalDateTime.now())
                .build();

        repository.save(subscriber);

        // Send welcome email
        emailService.sendWelcomeEmail(email);

        return ResponseEntity.ok(Map.of("message", "Subscription successful"));
    }
}