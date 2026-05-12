package com.final_year.v2.controller;

import com.final_year.v2.dto.DashboardStatsResponse;
import com.final_year.v2.dto.VideoResponse;
import com.final_year.v2.model.User;
import com.final_year.v2.repository.SubscriptionRepository;
import com.final_year.v2.repository.UserRepository;
import com.final_year.v2.repository.VideoRepository;
import com.final_year.v2.security.UserDetailsImpl;
import com.final_year.v2.service.EarningsService;
import com.final_year.v2.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/creator")
@PreAuthorize("hasRole('CREATOR') or hasRole('ADMIN')")
public class CreatorController {

    @Autowired
    private VideoService videoService;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EarningsService earningsService;

    @GetMapping("/videos")
    public ResponseEntity<Page<VideoResponse>> getMyVideos(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "publishedAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        Page<VideoResponse> videos = videoService.getCurrentUserVideos(currentUser.getId(), status, search, pageable);
        return ResponseEntity.ok(videos);
    }

    @DeleteMapping("/videos/{id}")
    public ResponseEntity<Void> deleteVideo(@PathVariable Long id,
                                            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        videoService.deleteVideoByOwner(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/dashboard/stats")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats(@AuthenticationPrincipal UserDetailsImpl currentUser) {
        Long userId = currentUser.getId();

        // Total views across all user's videos
        Long totalViews = videoRepository.sumViewsByUserId(userId);
        if (totalViews == null) totalViews = 0L;

        // Total likes across all user's videos
        Long totalLikes = videoRepository.sumLikesByUserId(userId);
        if (totalLikes == null) totalLikes = 0L;

        // Total earnings (from MonthlyEarnings)
        BigDecimal totalEarnings = earningsService.getTotalEarned(userId);
        if (totalEarnings == null) totalEarnings = BigDecimal.ZERO;

        // Subscriber count (users who subscribed to this creator)
        User creator = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        Long subscriberCount = subscriptionRepository.countBySubscribedTo(creator);

        return ResponseEntity.ok(new DashboardStatsResponse(totalViews, totalEarnings, subscriberCount, totalLikes));
    }
}