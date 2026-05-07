package com.final_year.v2.service;

import com.final_year.v2.dto.HistoryResponse;
import com.final_year.v2.model.History;
import com.final_year.v2.model.User;
import com.final_year.v2.model.Video;
import com.final_year.v2.repository.HistoryRepository;
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

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class HistoryService {

    private final HistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final VideoRepository videoRepository;

    @Transactional
    public void recordWatch(Long videoId) {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));

        // Check if already exists -> update timestamp
        historyRepository.findByUserIdAndVideoId(user.getId(), videoId)
                .ifPresentOrElse(
                        history -> historyRepository.updateWatchedAt(history.getId(), LocalDateTime.now()),
                        () -> {
                            History newHistory = History.builder()
                                    .user(user)
                                    .video(video)
                                    .watchedAt(LocalDateTime.now())
                                    .build();
                            historyRepository.save(newHistory);
                        }
                );
    }

    public Page<HistoryResponse> getUserHistory(Pageable pageable) {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return historyRepository.findByUserOrderByWatchedAtDesc(user, pageable)
                .map(this::mapToResponse);
    }

    @Transactional
    public void clearHistory() {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        historyRepository.deleteAllByUser(user);
    }

    private HistoryResponse mapToResponse(History history) {
        Video video = history.getVideo();
        return HistoryResponse.builder()
                .id(history.getId())
                .videoId(video.getId())
                .videoTitle(video.getTitle())
                .thumbnailUrl(video.getThumbnailUrl())
                .viewCount(video.getViewCount())
                .username(video.getUser().getUsername())
                .watchedAt(history.getWatchedAt())
                .paid(video.isPaid())
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