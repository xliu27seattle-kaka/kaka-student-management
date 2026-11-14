package com.kakastudio.sms.student.dto;

import com.kakastudio.sms.common.text.EmailNormalizer;
import com.kakastudio.sms.common.text.Texts;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** An administrator creating a student directly; the account is active at once. */
public record StudentCreateRequest(
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Size(max = 80) String firstName,
    @NotBlank @Size(max = 80) String lastName) {

  public StudentCreateRequest {
    email = EmailNormalizer.normalize(email);
    firstName = Texts.trim(firstName);
    lastName = Texts.trim(lastName);
  }
}
