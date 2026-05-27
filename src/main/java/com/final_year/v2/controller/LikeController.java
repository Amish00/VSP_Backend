package com.final_year.v2.controller;

import com.final_year.v2.dto.LikeStatusResponse;
import com.final_year.v2.security.UserDetailsImpl;
import com.final_year.v2.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/videos/{videoId}/like")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    @PostMapping
    public ResponseEntity<Void> like(@AuthenticationPrincipal UserDetailsImpl currentUser,
                                     @PathVariable Long videoId) {
        likeService.likeVideo(currentUser, videoId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> unlike(@AuthenticationPrincipal UserDetailsImpl currentUser,
                                       @PathVariable Long videoId) {
        likeService.unlikeVideo(currentUser, videoId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status")
    public ResponseEntity<LikeStatusResponse> getLikeStatus(@AuthenticationPrincipal UserDetailsImpl currentUser,
                                                            @PathVariable Long videoId) {
        boolean liked = currentUser != null && likeService.isLikedByUser(currentUser, videoId);
        long count = likeService.getLikeCount(videoId);
        return ResponseEntity.ok(new LikeStatusResponse(liked, count));
    }
}