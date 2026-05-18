package com.final_year.v2.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private Long id;
    private String title;
    private String message;
    private String type;
    private String relatedId;
    private boolean isRead;
    private LocalDateTime createdAt;
}