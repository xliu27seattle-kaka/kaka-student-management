package com.kakastudio.sms.graduation.dto;

import com.kakastudio.sms.student.domain.AcademicStatus;
import java.time.Instant;
import java.util.List;

/**
 * Where a student stands with graduation.
 *
 * @param reasons why the student cannot apply now; empty when {@code eligible}
 * @param latestApplication the most recent application, or {@code null}
 */
public record GraduationStatusResponse(
    Long studentId,
    String studentNo,
    String firstName,
    String lastName,
    AcademicStatus academicStatus,
    int earnedCredits,
    int requiredCredits,
    boolean eligible,
    List<String> reasons,
    ApplicationDto latestApplication,
    String certificateNo,
    Instant graduatedAt) {}
