package com.kakastudio.sms.student.dto;

import com.kakastudio.sms.student.domain.AcademicStatus;
import com.kakastudio.sms.user.domain.UserStatus;
import java.time.Instant;

public record StudentResponse(
    Long id,
    String studentNo,
    String email,
    String firstName,
    String lastName,
    UserStatus status,
    AcademicStatus academicStatus,
    Instant createdAt) {}
