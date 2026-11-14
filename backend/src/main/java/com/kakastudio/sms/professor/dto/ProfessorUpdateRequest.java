package com.kakastudio.sms.professor.dto;

import com.kakastudio.sms.common.text.EmailNormalizer;
import com.kakastudio.sms.common.text.Texts;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** An administrator correcting a professor's name or email. */
public record ProfessorUpdateRequest(
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Size(max = 80) String firstName,
    @NotBlank @Size(max = 80) String lastName) {

  public ProfessorUpdateRequest {
    email = EmailNormalizer.normalize(email);
    firstName = Texts.trim(firstName);
    lastName = Texts.trim(lastName);
  }
}
