package com.final_year.v2.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class SubscriptionResponse {
    private Long id;
    private Long subscribedToId;
    private String username;
    private String profilePicture;
    private LocalDateTime subscribedAt;
}