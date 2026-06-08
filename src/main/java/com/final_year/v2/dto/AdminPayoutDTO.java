package com.final_year.v2.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminPayoutDTO {
    private Long id;
    private BigDecimal amount;
    private String withdrawalMethod;
    private String accountDetails;
    private String status;
    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;
    private String rejectionReason;
    private CreatorSummary creator;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreatorSummary {
        private Long id;
        private String username;
        private String email;
    }
}