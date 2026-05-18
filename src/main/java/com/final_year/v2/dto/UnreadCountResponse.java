package com.final_year.v2.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UnreadCountResponse {
    private long unreadCount;
}