package com.final_year.v2.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaymentInitiateRequest {
    private String gateway;      // "esewa" or "khalti"
    private BigDecimal amount;   // base price (without tax)
    private String planId;       // "view" or "creator"
    private String billingCycle; // "monthly", "half", "yearly"
}