package com.final_year.v2.repository;

import com.final_year.v2.model.ViewRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ViewRecordRepository extends JpaRepository<ViewRecord, Long> {
    Optional<ViewRecord> findByUserIdAndVideoId(Long userId, Long videoId);
    List<ViewRecord> findAllByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT v.video.user.id as creatorId, SUM(v.userWeight) as weightedViews " +
            "FROM ViewRecord v WHERE v.createdAt BETWEEN :start AND :end GROUP BY v.video.user.id")
    List<Object[]> getWeightedViewsByCreator(@Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end);


    @Query(value = "SELECT DATE(v.created_at) as date, COUNT(v.id) as count " +
            "FROM view_records v WHERE v.video_user_id = :creatorId " +
            "AND v.created_at >= :startDate GROUP BY DATE(v.created_at) ORDER BY date",
            nativeQuery = true)
    List<Object[]> getDailyViewsForCreator(@Param("creatorId") Long creatorId,
                                           @Param("startDate") LocalDateTime startDate);

    long countByVideoUserIdAndCreatedAtAfter(Long userId, LocalDateTime date);
}