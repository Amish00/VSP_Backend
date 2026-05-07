package com.final_year.v2.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class HistoryResponse {
    private Long id;
    private Long videoId;
    private String videoTitle;
    private String thumbnailUrl;
    private Long viewCount;
    private String username;
    private LocalDateTime watchedAt;
    private Boolean paid;
}