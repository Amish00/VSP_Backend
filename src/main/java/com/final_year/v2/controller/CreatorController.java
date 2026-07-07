package com.final_year.v2.controller;

import com.final_year.v2.constaint.VideoStatus;
import com.final_year.v2.constaint.VideoType;
import com.final_year.v2.dto.DashboardStatsResponse;
import com.final_year.v2.dto.VideoResponse;
import com.final_year.v2.dto.VideoStatsResponse;
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
            @RequestParam(required = false) VideoType type,
            @PageableDefault(size = 10, sort = "publishedAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        Page<VideoResponse> videos = videoService.getCurrentUserVideos(
                currentUser.getId(), status, search, type, pageable);
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

    @GetMapping("/videos/stats")
    public ResponseEntity<VideoStatsResponse> getVideoStats(@AuthenticationPrincipal UserDetailsImpl currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Video counts
        long totalVideos = videoRepository.countByUserAndType(user, VideoType.VIDEO);
        long approvedVideos = videoRepository.countByUserAndStatusAndType(user, VideoStatus.APPROVED, VideoType.VIDEO);
        long pendingVideos = videoRepository.countByUserAndStatusAndType(user, VideoStatus.PENDING, VideoType.VIDEO);
        long rejectedVideos = videoRepository.countByUserAndStatusAndType(user, VideoStatus.REJECTED, VideoType.VIDEO);

        // Shorts counts
        long totalShorts = videoRepository.countByUserAndType(user, VideoType.SHORTS);
        long approvedShorts = videoRepository.countByUserAndStatusAndType(user, VideoStatus.APPROVED, VideoType.SHORTS);
        long pendingShorts = videoRepository.countByUserAndStatusAndType(user, VideoStatus.PENDING, VideoType.SHORTS);
        long rejectedShorts = videoRepository.countByUserAndStatusAndType(user, VideoStatus.REJECTED, VideoType.SHORTS);

        VideoStatsResponse response = VideoStatsResponse.builder()
                .totalVideos(totalVideos)
                .approvedVideos(approvedVideos)
                .pendingVideos(pendingVideos)
                .rejectedVideos(rejectedVideos)
                .totalShorts(totalShorts)
                .approvedShorts(approvedShorts)
                .pendingShorts(pendingShorts)
                .rejectedShorts(rejectedShorts)
                .build();

        return ResponseEntity.ok(response);
    }
}