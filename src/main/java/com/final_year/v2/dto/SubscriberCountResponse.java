package com.final_year.v2.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SubscriberCountResponse {
    private Long subscriberCount;
    private boolean isSubscribedByCurrentUser;
}