package com.final_year.v2.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class DashboardStatsResponse {
    private Long totalViews;
    private BigDecimal totalEarnings;
    private Long subscriberCount;
    private Long totalLikes;
}