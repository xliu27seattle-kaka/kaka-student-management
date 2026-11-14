package com.kakastudio.sms.professor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExperienceDto(
    @NotBlank @Size(max = 160) String roleTitle,
    @NotBlank @Size(max = 160) String organization,
    @NotBlank @Size(max = 60) String period) {}
