package com.final_year.v2.repository;

import com.final_year.v2.model.History;
import com.final_year.v2.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface HistoryRepository extends JpaRepository<History, Long> {

    Page<History> findByUserOrderByWatchedAtDesc(User user, Pageable pageable);

    Optional<History> findByUserIdAndVideoId(Long userId, Long videoId);

    @Modifying
    @Query("UPDATE History h SET h.watchedAt = :watchedAt WHERE h.id = :historyId")
    void updateWatchedAt(@Param("historyId") Long historyId, @Param("watchedAt") LocalDateTime watchedAt);

    @Modifying
    @Query("DELETE FROM History h WHERE h.user = :user")
    void deleteAllByUser(@Param("user") User user);
}