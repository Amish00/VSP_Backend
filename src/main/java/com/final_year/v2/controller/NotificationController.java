package com.final_year.v2.controller;

import com.final_year.v2.dto.NotificationResponse;
import com.final_year.v2.dto.UnreadCountResponse;
import com.final_year.v2.model.User;
import com.final_year.v2.repository.UserRepository;
import com.final_year.v2.security.UserDetailsImpl;
import com.final_year.v2.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            Pageable pageable) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(notificationService.getNotificationsForUser(user, pageable));
    }

    @PutMapping("/mark-read")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal UserDetailsImpl currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        notificationService.markAllAsRead(user);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(@AuthenticationPrincipal UserDetailsImpl currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        long count = notificationService.getUnreadCount(user);
        return ResponseEntity.ok(new UnreadCountResponse(count));
    }
}