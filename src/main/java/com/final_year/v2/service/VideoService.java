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
    private EngagementService engagementService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private LikeService likeService;  // Injected to check like status

    private static final Logger log = LoggerFactory.getLogger(VideoService.class);

    @Transactional
    public VideoResponse uploadVideo(VideoUploadRequest request,
                                     MultipartFile videoFile,
                                     MultipartFile thumbnailFile) {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String videoPublicId = "videos/" + UUID.randomUUID().toString();
        String thumbnailPublicId = "thumbnails/" + UUID.randomUUID().toString();

        String videoUrl = cloudinaryService.uploadFile(videoFile, "videos", videoPublicId);
        String thumbnailUrl = null;
        if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
            thumbnailUrl = cloudinaryService.uploadFile(thumbnailFile, "thumbnails", thumbnailPublicId);
        }

        Video video = new Video();
        video.setTitle(request.getTitle());
        video.setDescription(request.getDescription());
        video.setTags(request.getTags());
        video.setCategory(request.getCategory());
        video.setPaid(request.isPaid());
        video.setVideoUrl(videoUrl);
        video.setThumbnailUrl(thumbnailUrl);
        video.setType(request.getType());
        video.setStatus(VideoStatus.PENDING);
        video.setPublishedAt(LocalDateTime.now());
        video.setUpdatedAt(LocalDateTime.now());
        video.setUser(user);

        video = videoRepository.save(video);
        return convertToResponse(video, null);
    }

    @Transactional
    public VideoResponse getVideoById(Long id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found"));
        videoRepository.incrementViewCount(id);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl currentUser = null;
        if (auth != null && auth.isAuthenticated() && !(auth.getPrincipal() instanceof String && "anonymousUser".equals(auth.getPrincipal()))) {
            Object principal = auth.getPrincipal();
            if (principal instanceof UserDetailsImpl) {
                currentUser = (UserDetailsImpl) principal;
            }
            try {
                historyService.recordWatch(id);
                Long userId = paymentService.getCurrentUserId();
                engagementService.recordView(userId, id);
            } catch (RuntimeException e) {
                log.error("Failed to record watch history or view for video {}: {}", id, e.getMessage());
            }
        }

        return convertToResponse(video, currentUser);
    }

    public Page<VideoResponse> getAllVideos(Pageable pageable) {
        // For public listing, no current user -> likedByCurrentUser = false
        return videoRepository.findByStatus(VideoStatus.APPROVED, pageable)
                .map(video -> convertToResponse(video, null));
    }

    public Page<VideoResponse> getVideosByUser(String email, Pageable pageable) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl currentUser = getCurrentUserDetails(auth);
        return videoRepository.findByUser(user, pageable)
                .map(video -> convertToResponse(video, currentUser));
    }

    @Transactional
    public VideoResponse updateVideo(Long id, VideoUploadRequest request,
                                     MultipartFile newVideoFile,
                                     MultipartFile newThumbnailFile) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found"));

        String currentUserEmail = getCurrentUserEmail();
        if (!video.getUser().getEmail().equals(currentUserEmail)) {
            throw new RuntimeException("You are not authorized to update this video");
        }

        video.setTitle(request.getTitle());
        video.setDescription(request.getDescription());
        video.setTags(request.getTags());
        video.setCategory(request.getCategory());
        video.setPaid(request.isPaid());
        video.setType(request.getType());

        if (newVideoFile != null && !newVideoFile.isEmpty()) {
            String newVideoUrl = cloudinaryService.uploadFile(newVideoFile, "videos", UUID.randomUUID().toString());
            video.setVideoUrl(newVideoUrl);
        }
        if (newThumbnailFile != null && !newThumbnailFile.isEmpty()) {
            String newThumbUrl = cloudinaryService.uploadFile(newThumbnailFile, "thumbnails", UUID.randomUUID().toString());
            video.setThumbnailUrl(newThumbUrl);
        }

        video.setUpdatedAt(LocalDateTime.now());
        video = videoRepository.save(video);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl currentUser = getCurrentUserDetails(auth);
        return convertToResponse(video, currentUser);
    }

    @Transactional
    public void deleteVideo(Long id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found"));
        String currentUserEmail = getCurrentUserEmail();
        if (!video.getUser().getEmail().equals(currentUserEmail)) {
            throw new RuntimeException("Not authorized");
        }
        videoRepository.delete(video);
    }

    @Transactional
    public void updateVideoStatus(Long id, VideoStatus status) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found"));
        video.setStatus(status);
        videoRepository.save(video);
    }

    // Updated convertToResponse with currentUser parameter
    private VideoResponse convertToResponse(Video video, UserDetailsImpl currentUser) {
        VideoResponse response = VideoResponse.builder()
                .id(video.getId())
                .title(video.getTitle())
                .description(video.getDescription())
                .tags(video.getTags())
                .category(video.getCategory())
                .isPaid(video.isPaid())
                .videoUrl(video.getVideoUrl())
                .thumbnailUrl(video.getThumbnailUrl())
                .viewCount(video.getViewCount())
                .commentCount(video.getCommentCount())
                .likesCount(video.getLikesCount())
                .publishedAt(video.getPublishedAt())
                .updatedAt(video.getUpdatedAt())
                .type(video.getType())
                .status(video.getStatus())
                .username(video.getUser().getUsername())
                .profilePicture(video.getUser().getProfilePicture())
                .userEmail(video.getUser().getEmail())
                .userId(video.getUser().getId())
                .likedByCurrentUser(currentUser != null && likeService.isLikedByUser(currentUser, video.getId()))
                .build();
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

    private UserDetailsImpl getCurrentUserDetails(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() instanceof String) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetailsImpl) {
            return (UserDetailsImpl) principal;
        }
        return null;
    }

    private void notifyAdminOnUpload(Video video) {
        emailService.sendAdminNewVideoNotification(video, video.getUser());
    }

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
        // Admin listing: no need to check liked status (optional) but we pass null
        return videoPage.map(video -> convertToResponse(video, null));
    }

    @Transactional
    public void adminDeleteVideo(Long id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found"));
        videoRepository.delete(video);
    }

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
        // Admin update – no current user context for like status
        return convertToResponse(video, null);
    }

    public Page<VideoResponse> getSubscribedVideos(UserDetailsImpl currentUser, Pageable pageable) {
        return videoRepository.findVideosFromSubscribedChannels(currentUser.getId(), pageable)
                .map(video -> convertToResponse(video, currentUser));
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
        // For owner's own videos, we can get current user if needed; but we'll pass null because owner may not need liked status.
        return videoPage.map(video -> convertToResponse(video, null));
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