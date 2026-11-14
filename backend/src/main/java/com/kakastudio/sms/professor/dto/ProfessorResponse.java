package com.kakastudio.sms.professor.dto;

import com.kakastudio.sms.user.domain.UserStatus;

/** A professor as an administrator sees one in the management list. */
public record ProfessorResponse(
    Long id,
    String professorNo,
    String email,
    String firstName,
    String lastName,
    UserStatus status,
    String title) {}
