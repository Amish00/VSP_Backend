package com.final_year.v2.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payout_requests")
@Data
public class PayoutRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String withdrawalMethod; // "eSewa", "Khalti"

    @Column(length = 500)
    private String accountDetails; // eSewa ID, Khalti ID, etc.

    @Column(nullable = false)
    private String status; // "PENDING", "PROCESSED", "REJECTED"

    private LocalDateTime requestedAt = LocalDateTime.now();
    private LocalDateTime processedAt;
    private String rejectionReason;
}