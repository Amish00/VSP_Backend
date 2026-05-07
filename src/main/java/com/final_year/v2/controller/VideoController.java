package com.final_year.v2.controller;

import com.final_year.v2.constaint.VideoStatus;
import com.final_year.v2.dto.VideoResponse;
import com.final_year.v2.dto.VideoUploadRequest;
import com.final_year.v2.service.VideoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    @Autowired
    private VideoService videoService;

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<VideoResponse> uploadVideo(
            @Valid @RequestPart("data") VideoUploadRequest request,
            @RequestPart("video") MultipartFile videoFile,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnailFile) {
        VideoResponse response = videoService.uploadVideo(request, videoFile, thumbnailFile);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VideoResponse> getVideo(@PathVariable Long id) {
        return ResponseEntity.ok(videoService.getVideoById(id));
    }

    @GetMapping
    public ResponseEntity<Page<VideoResponse>> getAllVideos(Pageable pageable) {
        return ResponseEntity.ok(videoService.getAllVideos(pageable));
    }

    @GetMapping("/user/{email}")
    public ResponseEntity<Page<VideoResponse>> getUserVideos(@PathVariable String email, Pageable pageable) {
        return ResponseEntity.ok(videoService.getVideosByUser(email, pageable));
    }

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<VideoResponse> updateVideo(
            @PathVariable Long id,
            @Valid @RequestPart("data") VideoUploadRequest request,
            @RequestPart(value = "video", required = false) MultipartFile videoFile,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnailFile) {
        return ResponseEntity.ok(videoService.updateVideo(id, request, videoFile, thumbnailFile));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVideo(@PathVariable Long id) {
        videoService.deleteVideo(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateStatus(@PathVariable Long id, @RequestParam VideoStatus status) {
        videoService.updateVideoStatus(id, status);
        return ResponseEntity.ok().build();
    }
}