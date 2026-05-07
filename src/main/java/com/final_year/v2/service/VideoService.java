package com.final_year.v2.service;

import com.final_year.v2.constaint.VideoStatus;
import com.final_year.v2.dto.VideoResponse;
import com.final_year.v2.dto.VideoUploadRequest;
import com.final_year.v2.model.User;
import com.final_year.v2.model.Video;
import com.final_year.v2.repository.VideoRepository;
import com.final_year.v2.repository.UserRepository;
import com.final_year.v2.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class VideoService {

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private EngagementService  engagementService;

    @Autowired
    private PaymentService paymentService;

    private static final Logger log = LoggerFactory.getLogger(VideoService.class);

    @Transactional
    public VideoResponse uploadVideo(VideoUploadRequest request,
                                     MultipartFile videoFile,
                                     MultipartFile thumbnailFile) {
        // Get current logged-in user
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate unique ID for video and thumbnail
        String videoPublicId = "videos/" + UUID.randomUUID().toString();
        String thumbnailPublicId = "thumbnails/" + UUID.randomUUID().toString();

        // Upload files to Cloudinary
        String videoUrl = cloudinaryService.uploadFile(videoFile, "videos", videoPublicId);
        String thumbnailUrl = null;
        if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
            thumbnailUrl = cloudinaryService.uploadFile(thumbnailFile, "thumbnails", thumbnailPublicId);
        }

        // Create Video entity
        Video video = new Video();
        video.setTitle(request.getTitle());
        video.setDescription(request.getDescription());
        video.setTags(request.getTags());
        video.setCategory(request.getCategory());
        video.setPaid(request.isPaid());
        video.setVideoUrl(videoUrl);
        video.setThumbnailUrl(thumbnailUrl);
        video.setType(request.getType());
        video.setStatus(VideoStatus.PENDING); // default pending
        video.setPublishedAt(LocalDateTime.now());
        video.setUpdatedAt(LocalDateTime.now());
        video.setUser(user);

        video = videoRepository.save(video);
        return convertToResponse(video);
    }

    @Transactional
    public VideoResponse getVideoById(Long id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found"));
        videoRepository.incrementViewCount(id);

        // Record watch only for fully authenticated users (not anonymous)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth.getPrincipal() instanceof String && "anonymousUser".equals(auth.getPrincipal()))) {
            try {
                historyService.recordWatch(id);
                Long userId = paymentService.getCurrentUserId();
                engagementService.recordView(userId,id);
            } catch (RuntimeException e) {
                // Log the error but do not let it affect the main transaction
                log.error("Failed to record watch history or view for video {}: {}", id, e.getMessage());
            }
        }

        return convertToResponse(video);
    }

    public Page<VideoResponse> getAllVideos(Pageable pageable) {
        return videoRepository.findByStatus(VideoStatus.APPROVED, pageable)
                .map(this::convertToResponse);
    }

    public Page<VideoResponse> getVideosByUser(String email, Pageable pageable) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return videoRepository.findByUser(user, pageable)
                .map(this::convertToResponse);
    }

    @Transactional
    public VideoResponse updateVideo(Long id, VideoUploadRequest request,
                                     MultipartFile newVideoFile,
                                     MultipartFile newThumbnailFile) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found"));

        // Check ownership (or admin)
        String currentUserEmail = getCurrentUserEmail();
        if (!video.getUser().getEmail().equals(currentUserEmail)) {
            throw new RuntimeException("You are not authorized to update this video");
        }

        // Update metadata
        video.setTitle(request.getTitle());
        video.setDescription(request.getDescription());
        video.setTags(request.getTags());
        video.setCategory(request.getCategory());
        video.setPaid(request.isPaid());
        video.setType(request.getType());

        // Update files if provided
        if (newVideoFile != null && !newVideoFile.isEmpty()) {
            // Delete old from Cloudinary (optional)
            // cloudinaryService.deleteFile(oldPublicId, "video");
            String newVideoUrl = cloudinaryService.uploadFile(newVideoFile, "videos", UUID.randomUUID().toString());
            video.setVideoUrl(newVideoUrl);
        }
        if (newThumbnailFile != null && !newThumbnailFile.isEmpty()) {
            String newThumbUrl = cloudinaryService.uploadFile(newThumbnailFile, "thumbnails", UUID.randomUUID().toString());
            video.setThumbnailUrl(newThumbUrl);
        }

        video.setUpdatedAt(LocalDateTime.now());
        video = videoRepository.save(video);
        return convertToResponse(video);
    }

    @Transactional
    public void deleteVideo(Long id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found"));
        // Check ownership or admin
        String currentUserEmail = getCurrentUserEmail();
        if (!video.getUser().getEmail().equals(currentUserEmail)) {
            throw new RuntimeException("Not authorized");
        }
        videoRepository.delete(video);
        // Optionally delete from Cloudinary (extract public ID from URL)
    }

    @Transactional
    public void updateVideoStatus(Long id, VideoStatus status) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found"));
        video.setStatus(status);
        videoRepository.save(video);
    }

    private VideoResponse convertToResponse(Video video) {
        VideoResponse response = new VideoResponse();
        response.setId(video.getId());
        response.setTitle(video.getTitle());
        response.setDescription(video.getDescription());
        response.setTags(video.getTags());
        response.setCategory(video.getCategory());
        response.setPaid(video.isPaid());
        response.setVideoUrl(video.getVideoUrl());
        response.setThumbnailUrl(video.getThumbnailUrl());
        response.setViewCount(video.getViewCount());
        response.setCommentCount(video.getCommentCount());
        response.setLikesCount(video.getLikesCount());
        response.setPublishedAt(video.getPublishedAt());
        response.setUpdatedAt(video.getUpdatedAt());
        response.setType(video.getType());
        response.setStatus(video.getStatus());
        response.setProfilePicture(video.getUser().getProfilePicture());
        response.setUsername(video.getUser().getUsername());
        response.setUserEmail(video.getUser().getEmail());
        response.setCommentCount(video.getCommentCount());
        response.setUserId(video.getUser().getId());
        return response;
    }

    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new UsernameNotFoundException("User not found");
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetailsImpl userDetails) {
            return userDetails.getEmail().toLowerCase();
        }

        return auth.getName().toLowerCase();
    }



    // Call this inside uploadVideo() after saving the video
    private void notifyAdminOnUpload(Video video) {
        emailService.sendAdminNewVideoNotification(video, video.getUser());
    }

    // Modified status update with reason and email
    @Transactional
    public void updateVideoStatus(Long id, VideoStatus status, String rejectionReason) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found"));
        video.setStatus(status);
        if (status == VideoStatus.REJECTED) {
            video.setRejectionReason(rejectionReason);
            emailService.sendVideoRejectedNotification(video, video.getUser(), rejectionReason);
        } else if (status == VideoStatus.APPROVED) {
            emailService.sendVideoApprovedNotification(video, video.getUser());
        }
        videoRepository.save(video);
    }

    // Admin: get all videos with optional filter and search
    public Page<VideoResponse> getAllVideosForAdmin(String status, String search, Pageable pageable) {
        Page<Video> videoPage;
        VideoStatus videoStatus = null;
        if (status != null && !status.isEmpty() && !"All".equals(status)) {
            videoStatus = VideoStatus.valueOf(status);
        }
        if (search != null && !search.isEmpty()) {
            videoPage = videoRepository.searchAllVideos(videoStatus, search, pageable);
        } else if (videoStatus != null) {
            videoPage = videoRepository.findAllByStatus(videoStatus, pageable);
        } else {
            videoPage = videoRepository.findAll(pageable);
        }
        return videoPage.map(this::convertToResponse);
    }

    // Admin: delete any video
    @Transactional
    public void adminDeleteVideo(Long id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found"));
        videoRepository.delete(video);
    }

    // Admin: update video metadata (without file upload)
    @Transactional
    public VideoResponse adminUpdateVideo(Long id, VideoUploadRequest request) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found"));
        video.setTitle(request.getTitle());
        video.setDescription(request.getDescription());
        video.setTags(request.getTags());
        video.setCategory(request.getCategory());
        video.setPaid(request.isPaid());
        video.setType(request.getType());
        video.setUpdatedAt(LocalDateTime.now());
        video = videoRepository.save(video);
        return convertToResponse(video);
    }

    public Page<VideoResponse> getSubscribedVideos(UserDetailsImpl currentUser, Pageable pageable) {
        return videoRepository.findVideosFromSubscribedChannels(currentUser.getId(), pageable)
                .map(this::convertToResponse);
    }

    public Page<VideoResponse> getCurrentUserVideos(Long userId, String status, String search, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        VideoStatus videoStatus = null;
        if (status != null && !status.isEmpty() && !"All".equals(status)) {
            try {
                videoStatus = VideoStatus.valueOf(status);
            } catch (IllegalArgumentException ignored) {}
        }
        Page<Video> videoPage = videoRepository.findByUserAndFilters(user, videoStatus, search, pageable);
        return videoPage.map(this::convertToResponse);
    }

    @Transactional
    public void deleteVideoByOwner(Long videoId, Long userId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));
        if (!video.getUser().getId().equals(userId)) {
            throw new RuntimeException("You are not authorized to delete this video");
        }
        videoRepository.delete(video);
    }
}