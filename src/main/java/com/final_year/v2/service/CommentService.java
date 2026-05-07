package com.final_year.v2.service;

import com.final_year.v2.dto.CommentRequest;
import com.final_year.v2.dto.CommentResponse;
import com.final_year.v2.model.Comment;
import com.final_year.v2.model.User;
import com.final_year.v2.model.Video;
import com.final_year.v2.repository.CommentRepository;
import com.final_year.v2.repository.UserRepository;
import com.final_year.v2.repository.VideoRepository;
import com.final_year.v2.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final VideoRepository videoRepository;

    @Transactional
    public CommentResponse addComment(CommentRequest request) {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Video video = videoRepository.findById(request.getVideoId())
                .orElseThrow(() -> new RuntimeException("Video not found"));

        Comment comment = Comment.builder()
                .content(request.getContent())
                .video(video)
                .user(user)
                .likesCount(0L)
                .build();
        comment = commentRepository.save(comment);

        // Increment comment count in Video entity
        videoRepository.incrementCommentCount(video.getId());

        return mapToResponse(comment);
    }

    public Page<CommentResponse> getCommentsByVideo(Long videoId, Pageable pageable) {
        return commentRepository.findByVideoIdOrderByCreatedAtDesc(videoId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional
    public void deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        String currentUserEmail = getCurrentUserEmail();
        if (!comment.getUser().getEmail().equals(currentUserEmail)) {
            throw new RuntimeException("Not authorized to delete this comment");
        }
        commentRepository.delete(comment);
        // Decrement comment count in Video
        videoRepository.decrementCommentCount(comment.getVideo().getId());
    }

    @Transactional
    public void likeComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        commentRepository.incrementLikeCount(commentId);
        // Optional: track which user liked (avoid duplicates) – not implemented here for brevity
    }

    @Transactional
    public void unlikeComment(Long commentId) {
        commentRepository.decrementLikeCount(commentId);
    }

    private CommentResponse mapToResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .username(comment.getUser().getUsername())
                .userProfilePicture(comment.getUser().getProfilePicture())
                .likesCount(comment.getLikesCount())
                .build();
    }

    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new RuntimeException("User not authenticated");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetailsImpl userDetails) {
            return userDetails.getEmail().toLowerCase();
        }
        return auth.getName().toLowerCase();
    }
}