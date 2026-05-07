package com.final_year.v2.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentResponse {
    private Long id;
    private String username;
    private String paymentMethod; // gateway
    private String transactionId;
    private BigDecimal amount;
    private String planId;
    private String billingCycle;
    private String status;
    private LocalDateTime transactionDate; // createdAt
}