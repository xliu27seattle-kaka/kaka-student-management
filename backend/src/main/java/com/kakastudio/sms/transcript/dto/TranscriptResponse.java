package com.kakastudio.sms.transcript.dto;

import com.kakastudio.sms.student.domain.AcademicStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * A student's academic record.
 *
 * @param official {@code true} once the student has graduated; before that the transcript is
 *     unofficial
 * @param cumulativeGpa {@code null} until the first graded credit
 */
public record TranscriptResponse(
    Long studentId,
    String studentNo,
    String firstName,
    String lastName,
    String email,
    AcademicStatus academicStatus,
    String certificateNo,
    Instant graduatedAt,
    boolean official,
    BigDecimal cumulativeGpa,
    int earnedCredits,
    List<TermRecord> terms) {}
