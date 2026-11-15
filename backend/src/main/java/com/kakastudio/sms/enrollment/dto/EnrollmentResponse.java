package com.kakastudio.sms.enrollment.dto;

import com.kakastudio.sms.enrollment.domain.EnrollmentStatus;
import com.kakastudio.sms.section.dto.SectionResponse;

public record EnrollmentResponse(
    Long id, EnrollmentStatus status, SectionResponse section, Integer score, String letterGrade) {}
