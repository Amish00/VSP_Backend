package com.final_year.v2.dto;

import com.final_year.v2.constaint.VideoStatus;
import com.final_year.v2.constaint.VideoType;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoResponse {
    private Long id;
    private String title;
    private String description;
    private String tags;
    private String category;
    private boolean isPaid;
    private String videoUrl;
    private String thumbnailUrl;
    private Long viewCount;
    private Long commentCount;
    private Long likesCount;
    private LocalDateTime publishedAt;
    private LocalDateTime updatedAt;
    private VideoType type;
    private VideoStatus status;
    private String username;
    private String profilePicture;
    private String userEmail;
    private Long userId;
}