package com.final_year.v2.repository;

import com.final_year.v2.model.WatchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface WatchHistoryRepository extends JpaRepository<WatchHistory, Long> {
    List<WatchHistory> findAllByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT w.video.user.id as creatorId, SUM(w.watchTimeSeconds * w.userWeight) as weightedWatchTime " +
            "FROM WatchHistory w WHERE w.createdAt BETWEEN :start AND :end GROUP BY w.video.user.id")
    List<Object[]> getWeightedWatchTimeByCreator(@Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end);
}