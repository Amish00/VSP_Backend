package com.final_year.v2.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "monthly_earnings")
@Data
@NoArgsConstructor
public class MonthlyEarnings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long creatorId;
    private String monthYear; // "YYYY-MM"
    private BigDecimal earningsAmount;
    private LocalDateTime calculatedAt = LocalDateTime.now();
}