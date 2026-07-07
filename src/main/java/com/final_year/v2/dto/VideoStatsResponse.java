package com.final_year.v2.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoStatsResponse {
    // Video stats
    private long totalVideos;
    private long approvedVideos;
    private long pendingVideos;
    private long rejectedVideos;

    // Shorts stats
    private long totalShorts;
    private long approvedShorts;
    private long pendingShorts;
    private long rejectedShorts;
}