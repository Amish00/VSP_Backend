package com.final_year.v2.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "revenue_records")
@Data
@NoArgsConstructor
public class RevenueRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String username; // denormalized for fast query

    @Column(nullable = false)
    private String paymentMethod; // "esewa" or "khalti"

    @Column(nullable = false)
    private String transactionId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String planType; // "view" or "creator"

    @Column(nullable = false)
    private String billingCycle; // "monthly", "half", "yearly"

    @Column(nullable = false)
    private LocalDateTime transactionDate;

    @Column(nullable = false)
    private LocalDateTime subscriptionStartDate;

    @Column(nullable = false)
    private LocalDateTime subscriptionEndDate;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}