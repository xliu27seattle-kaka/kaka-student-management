package com.kakastudio.sms.transcript.dto;

import com.kakastudio.sms.semester.domain.SemesterStatus;
import java.math.BigDecimal;
import java.util.List;

/**
 * One semester of a transcript.
 *
 * @param gpa {@code null} when nothing in the semester is graded yet
 */
public record TermRecord(
    Long semesterId,
    String semesterCode,
    String semesterName,
    SemesterStatus semesterStatus,
    BigDecimal gpa,
    int gradedCredits,
    List<TranscriptRow> rows) {}
