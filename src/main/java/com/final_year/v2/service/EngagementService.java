package com.final_year.v2.service;

import com.final_year.v2.constaint.VideoStatus;
import com.final_year.v2.model.*;
import com.final_year.v2.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class EngagementService {

    @Autowired
    private WatchHistoryRepository watchHistoryRepository;
    @Autowired
    private ViewRecordRepository viewRecordRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private VideoRepository videoRepository;

    private static final int MAX_WATCH_SECONDS = 600;

    @Transactional
    public void recordWatchTime(Long userId, Long videoId, int seconds) {
        if (seconds <= 0) return;
        if (seconds > MAX_WATCH_SECONDS) seconds = MAX_WATCH_SECONDS;

        User user = userRepository.findById(userId).orElseThrow();
        Video video = videoRepository.findById(videoId).orElseThrow();

        BigDecimal weight = isPaidUser(user) ? BigDecimal.ONE : new BigDecimal("0.5");

        WatchHistory wh = new WatchHistory();
        wh.setUser(user);
        wh.setVideo(video);
        wh.setWatchTimeSeconds(seconds);
        wh.setUserWeight(weight);
        wh.setCreatedAt(LocalDateTime.now());
        watchHistoryRepository.save(wh);
    }

    @Transactional
    public boolean recordView(Long userId, String sessionId, Long videoId) {
        Video video = videoRepository.findById(videoId).orElse(null);
        if (video == null || video.getStatus() != VideoStatus.APPROVED) {
            return false;
        }

        if (userId != null && video.getUser().getId().equals(userId)) {
            return false;
        }

        if (userId != null) {
            Optional<ViewRecord> existing = viewRecordRepository.findByUserIdAndVideoId(userId, videoId);
            if (existing.isPresent()) {
                return false;
            }
        } else if (sessionId != null) {
            Optional<ViewRecord> existing = viewRecordRepository.findBySessionIdAndVideoId(sessionId, videoId);
            if (existing.isPresent()) {
                return false;
            }
        } else {
            return false;
        }

        // Determine weight
        BigDecimal weight;
        if (userId != null) {
            User user = userRepository.findById(userId).orElseThrow();
            weight = isPaidUser(user) ? BigDecimal.ONE : new BigDecimal("0.5");
        } else {
            weight = new BigDecimal("0.5"); // anonymous = free tier
        }

        ViewRecord vr = new ViewRecord();
        if (userId != null) {
            vr.setUser(userRepository.getReferenceById(userId));
        } else {
            vr.setSessionId(sessionId);
        }
        vr.setVideo(video);
        vr.setUserWeight(weight);
        vr.setCreatedAt(LocalDateTime.now());

        try {
            viewRecordRepository.save(vr);
            videoRepository.incrementViewCount(videoId);
            return true;
        } catch (DataIntegrityViolationException e) {
            // Duplicate key (race condition) – another request already created the record
            return false;
        }
    }

    private boolean isPaidUser(User user) {
        return user.getSubscriptionExpiry() != null &&
                user.getSubscriptionExpiry().isAfter(LocalDateTime.now()) &&
                user.getPlan() != com.final_year.v2.constaint.Plan.FREE;
    }
}