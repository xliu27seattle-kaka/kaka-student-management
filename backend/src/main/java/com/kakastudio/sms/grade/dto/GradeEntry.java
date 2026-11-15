package com.kakastudio.sms.grade.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** A score for one enrollment; {@code null} clears it. */
public record GradeEntry(@NotNull Long enrollmentId, @Min(0) @Max(100) Integer score) {}
