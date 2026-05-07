package com.final_year.v2.dto;

import com.final_year.v2.constaint.VideoStatus;
import jakarta.validation.constraints.NotNull;

public class VideoStatusUpdateRequest {
    @NotNull
    private VideoStatus status;
    private String rejectionReason;  // required if status == REJECTED

    public VideoStatus getStatus() {
        return status;
    }

    public void setStatus(VideoStatus status) {
        this.status = status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}