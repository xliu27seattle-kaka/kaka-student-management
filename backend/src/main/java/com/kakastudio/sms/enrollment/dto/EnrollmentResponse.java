package com.kakastudio.sms.enrollment.dto;

import com.kakastudio.sms.enrollment.domain.EnrollmentStatus;
import com.kakastudio.sms.section.dto.CourseSectionResponse;

public record EnrollmentResponse(
    Long id,
    EnrollmentStatus status,
    CourseSectionResponse section,
    Integer score,
    String letterGrade) {}
