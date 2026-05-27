package com.final_year.v2.controller;

import com.final_year.v2.dto.SubscriberCountResponse;
import com.final_year.v2.dto.SubscriptionResponse;
import com.final_year.v2.dto.VideoResponse;
import com.final_year.v2.security.UserDetailsImpl;
import com.final_year.v2.service.SubscriptionService;
import com.final_year.v2.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final VideoService videoService;

    @PostMapping("/{creatorId}")
    public ResponseEntity<Void> subscribe(@AuthenticationPrincipal UserDetailsImpl currentUser,
                                          @PathVariable Long creatorId) {
        subscriptionService.subscribe(currentUser, creatorId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{creatorId}")
    public ResponseEntity<Void> unsubscribe(@AuthenticationPrincipal UserDetailsImpl currentUser,
                                            @PathVariable Long creatorId) {
        subscriptionService.unsubscribe(currentUser, creatorId);
        return ResponseEntity.noContent().build();
    }

    // Updated: currentUser is optional (null if not logged in)
    @GetMapping("/{creatorId}/info")
    public ResponseEntity<SubscriberCountResponse> getSubscriberInfo(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @PathVariable Long creatorId) {
        return ResponseEntity.ok(subscriptionService.getSubscriberInfo(creatorId, currentUser));
    }

    @GetMapping("/me")
    public ResponseEntity<Page<SubscriptionResponse>> getMySubscriptions(@AuthenticationPrincipal UserDetailsImpl currentUser,
                                                                         Pageable pageable) {
        return ResponseEntity.ok(subscriptionService.getSubscribedChannels(currentUser, pageable));
    }

    @GetMapping("/subscribed")
    public ResponseEntity<Page<VideoResponse>> getSubscribedVideos(@AuthenticationPrincipal UserDetailsImpl currentUser,
                                                                   Pageable pageable) {
        return ResponseEntity.ok(videoService.getSubscribedVideos(currentUser, pageable));
    }
}