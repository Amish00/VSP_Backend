package com.final_year.v2.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface PaymentProjection {
    Long getId();
    String getUsername();
    String getPaymentMethod();  // maps to gateway
    String getTransactionId();
    BigDecimal getAmount();
    String getPlanId();
    String getBillingCycle();
    String getStatus();
    LocalDateTime getTransactionDate(); // maps to createdAt
}