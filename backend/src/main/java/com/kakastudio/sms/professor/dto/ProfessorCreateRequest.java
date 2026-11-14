package com.kakastudio.sms.professor.dto;

import com.kakastudio.sms.common.text.EmailNormalizer;
import com.kakastudio.sms.common.text.Texts;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** An administrator creating a professor. The profile may be left for the professor to fill in. */
public record ProfessorCreateRequest(
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Size(max = 80) String firstName,
    @NotBlank @Size(max = 80) String lastName,
    @Valid ProfessorProfileRequest profile) {

  public ProfessorCreateRequest {
    email = EmailNormalizer.normalize(email);
    firstName = Texts.trim(firstName);
    lastName = Texts.trim(lastName);
  }
}
