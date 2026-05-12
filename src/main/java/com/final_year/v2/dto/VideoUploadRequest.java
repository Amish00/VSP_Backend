package com.final_year.v2.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.final_year.v2.constaint.VideoType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VideoUploadRequest {
    @NotBlank
    private String title;
    private String description;
    private String tags;
    private String category;

    @JsonAlias({"paid", "isPaid"})      // maps JSON "isPaid" to this field
    private boolean paid = false;

    @NotNull
    private VideoType type;
}