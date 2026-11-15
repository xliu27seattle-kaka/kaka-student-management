package com.kakastudio.sms.graduation.dto;

import com.kakastudio.sms.graduation.domain.ApplicationStatus;
import java.time.Instant;

public record ApplicationDto(
    Long id,
    Long studentId,
    String studentNo,
    String studentName,
    String email,
    ApplicationStatus status,
    Instant appliedAt,
    Instant decidedAt,
    int earnedCredits) {}
