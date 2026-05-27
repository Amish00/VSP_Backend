package com.final_year.v2.service;

import com.final_year.v2.model.Like;
import com.final_year.v2.model.User;
import com.final_year.v2.model.Video;
import com.final_year.v2.repository.LikeRepository;
import com.final_year.v2.repository.UserRepository;
import com.final_year.v2.repository.VideoRepository;
import com.final_year.v2.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;
    private final UserRepository userRepository;
    private final VideoRepository videoRepository;

    @Transactional
    public void likeVideo(UserDetailsImpl currentUser, Long videoId) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));

        if (likeRepository.existsByUserAndVideo(user, video)) {
            throw new RuntimeException("Already liked");
        }

        Like like = Like.builder()
                .user(user)
                .video(video)
                .build();
        likeRepository.save(like);
        videoRepository.incrementLikeCount(videoId);
    }

    @Transactional
    public void unlikeVideo(UserDetailsImpl currentUser, Long videoId) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));

        likeRepository.deleteByUserAndVideo(user, video);
        videoRepository.decrementLikeCount(videoId);
    }

    public boolean isLikedByUser(UserDetailsImpl currentUser, Long videoId) {
        if (currentUser == null) return false;
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));
        return likeRepository.existsByUserAndVideo(user, video);
    }

    public long getLikeCount(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));
        return likeRepository.countByVideo(video);
    }
}