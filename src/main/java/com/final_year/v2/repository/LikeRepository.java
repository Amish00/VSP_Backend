package com.final_year.v2.repository;

import com.final_year.v2.model.Like;
import com.final_year.v2.model.User;
import com.final_year.v2.model.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {
    Optional<Like> findByUserAndVideo(User user, Video video);
    boolean existsByUserAndVideo(User user, Video video);
    void deleteByUserAndVideo(User user, Video video);
    long countByVideo(Video video);
}