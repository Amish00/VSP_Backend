package com.final_year.v2.model;

import com.final_year.v2.constaint.VideoStatus;
import com.final_year.v2.constaint.VideoType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "videos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 5000)
    private String description;

    private String tags;
    private String category;
    private boolean isPaid = false;

    @Column(nullable = false)
    private String videoUrl;

    private String thumbnailUrl;

    private Long viewCount = 0L;
    private Long commentCount = 0L;
    private Long likesCount = 0L;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private LocalDateTime publishedAt;

    @Enumerated(EnumType.STRING)
    private VideoType type;

    @Enumerated(EnumType.STRING)
    private VideoStatus status = VideoStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String rejectionReason;
}