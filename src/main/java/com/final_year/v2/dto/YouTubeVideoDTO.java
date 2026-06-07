package com.final_year.v2.dto;

import lombok.Data;

@Data
public class YouTubeVideoDTO {
    private String id;
    private String title;
    private String thumbnailUrl;
    private String channelTitle;
    private String publishedAt;
    private String channelId;
    private String profilePictureUrl;
    private Long viewCount;
}
