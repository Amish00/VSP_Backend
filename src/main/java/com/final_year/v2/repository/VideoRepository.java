package com.final_year.v2.repository;

import com.final_year.v2.constaint.VideoStatus;
import com.final_year.v2.model.Video;
import com.final_year.v2.model.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VideoRepository extends JpaRepository<Video, Long> {
    Page<Video> findByStatus(VideoStatus status, Pageable pageable);
    Page<Video> findByUser(User user, Pageable pageable);
    Page<Video> findByCategory(String category, Pageable pageable);

    @Modifying
    @Query("UPDATE Video v SET v.viewCount = v.viewCount + 1 WHERE v.id = :videoId")
    void incrementViewCount(@Param("videoId") Long videoId);

    @Modifying
    @Query("UPDATE Video v SET v.commentCount = v.commentCount + 1 WHERE v.id = :videoId")
    void incrementCommentCount(@Param("videoId") Long videoId);

    @Modifying
    @Query("UPDATE Video v SET v.likesCount = v.likesCount + 1 WHERE v.id = :videoId")
    void incrementLikeCount(@Param("videoId") Long videoId);

    @Modifying
    @Query("UPDATE Video v SET v.likesCount = v.likesCount - 1 WHERE v.id = :videoId")
    void decrementLikeCount(@Param("videoId") Long videoId);

    @Query("SELECT v FROM Video v WHERE " +
            "(COALESCE(:status, NULL) IS NULL OR v.status = :status) AND " +
            "(COALESCE(:search, NULL) IS NULL OR " +
            "LOWER(v.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(v.user.username) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Video> searchAllVideos(@Param("status") VideoStatus status,
                                @Param("search") String search,
                                Pageable pageable);

    Page<Video> findAllByStatus(VideoStatus status, Pageable pageable);


    @Modifying
    @Query("UPDATE Video v SET v.commentCount = v.commentCount - 1 WHERE v.id = :videoId")
    void decrementCommentCount(@Param("videoId") Long videoId);

    @Query("SELECT v FROM Video v WHERE v.user.id IN (SELECT s.subscribedTo.id FROM Subscription s WHERE s.subscriber.id = :userId) AND v.status = 'APPROVED' ORDER BY v.publishedAt DESC")
    Page<Video> findVideosFromSubscribedChannels(@Param("userId") Long userId, Pageable pageable);

    // Add this method to VideoRepository.java

    @Query("SELECT v FROM Video v WHERE v.user = :user " +
            "AND (:status IS NULL OR v.status = :status) " +
            "AND (:search IS NULL OR LOWER(v.title) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Video> findByUserAndFilters(@Param("user") User user,
                                     @Param("status") VideoStatus status,
                                     @Param("search") String search,
                                     Pageable pageable);
}