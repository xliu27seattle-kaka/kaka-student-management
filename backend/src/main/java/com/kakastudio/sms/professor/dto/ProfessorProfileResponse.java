package com.kakastudio.sms.professor.dto;

import java.util.List;

/**
 * The profile page every signed-in user may read. It deliberately has no email address: contact
 * details stay with administrators.
 */
public record ProfessorProfileResponse(
    Long id,
    String professorNo,
    String firstName,
    String lastName,
    String title,
    String bio,
    String photoUrl,
    List<String> expertise,
    List<ExperienceDto> experience,
    List<AwardDto> awards) {}
