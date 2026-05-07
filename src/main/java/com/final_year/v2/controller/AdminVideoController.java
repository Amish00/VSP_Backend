package com.final_year.v2.controller;

import com.final_year.v2.dto.VideoResponse;
import com.final_year.v2.dto.VideoStatusUpdateRequest;
import com.final_year.v2.dto.VideoUploadRequest;
import com.final_year.v2.service.VideoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/videos")
@PreAuthorize("hasRole('ADMIN')")
public class AdminVideoController {

    @Autowired
    private VideoService videoService;

    @GetMapping
    public ResponseEntity<Page<VideoResponse>> getAllVideos(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        return ResponseEntity.ok(videoService.getAllVideosForAdmin(status, search, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VideoResponse> updateVideo(@PathVariable Long id,
                                                     @Valid @RequestBody VideoUploadRequest request) {
        return ResponseEntity.ok(videoService.adminUpdateVideo(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVideo(@PathVariable Long id) {
        videoService.adminDeleteVideo(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable Long id,
                                             @Valid @RequestBody VideoStatusUpdateRequest request) {
        videoService.updateVideoStatus(id, request.getStatus(), request.getRejectionReason());
        return ResponseEntity.ok().build();
    }
}