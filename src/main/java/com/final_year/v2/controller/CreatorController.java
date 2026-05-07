package com.final_year.v2.controller;

import com.final_year.v2.dto.VideoResponse;
import com.final_year.v2.security.UserDetailsImpl;
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

@RestController
@RequestMapping("/api/creator")
@PreAuthorize("hasRole('CREATOR') or hasRole('ADMIN')")
public class CreatorController {

    @Autowired
    private VideoService videoService;

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
}