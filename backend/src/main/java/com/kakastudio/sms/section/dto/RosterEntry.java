package com.kakastudio.sms.section.dto;

import com.kakastudio.sms.enrollment.domain.EnrollmentStatus;

/** One student in a section's class list. */
public record RosterEntry(
    Long enrollmentId,
    Long studentId,
    String studentNo,
    String firstName,
    String lastName,
    String email,
    EnrollmentStatus status,
    Integer score,
    String letterGrade) {}
