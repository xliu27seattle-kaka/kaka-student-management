package com.kakastudio.sms.semester.dto;

import com.kakastudio.sms.semester.domain.SemesterStatus;
import java.time.LocalDate;
import java.util.Set;

public record SemesterResponse(
    Long id,
    String code,
    String name,
    LocalDate startDate,
    LocalDate endDate,
    SemesterStatus status,
    Set<SemesterStatus> allowedNext,
    long sectionCount,
    long enrolledCount) {}
