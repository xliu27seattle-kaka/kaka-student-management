package com.kakastudio.sms.professor.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AwardDto(@NotBlank @Size(max = 200) String title, @Min(1900) @Max(2100) int year) {}
