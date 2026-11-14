package com.kakastudio.sms.professor.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/** The self-introduction a professor (or an administrator creating one) fills in. */
public record ProfessorProfileRequest(
    @Size(max = 120) String title,
    @Size(max = 4000) String bio,
    @Size(max = 12) List<@NotBlank @Size(max = 120) String> expertise,
    @Size(max = 20) @Valid List<ExperienceDto> experience,
    @Size(max = 20) @Valid List<AwardDto> awards) {

  /** A list that was left out counts as empty. */
  public ProfessorProfileRequest {
    expertise = expertise == null ? List.of() : List.copyOf(expertise);
    experience = experience == null ? List.of() : List.copyOf(experience);
    awards = awards == null ? List.of() : List.copyOf(awards);
  }
}
