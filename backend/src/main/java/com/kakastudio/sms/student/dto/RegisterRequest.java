package com.kakastudio.sms.student.dto;

import com.kakastudio.sms.common.text.EmailNormalizer;
import com.kakastudio.sms.common.text.Texts;
import com.kakastudio.sms.user.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** What a new student fills in on the registration page. */
public record RegisterRequest(
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Size(max = 80) String firstName,
    @NotBlank @Size(max = 80) String lastName,
    @NotBlank @ValidPassword String password) {

  public RegisterRequest {
    email = EmailNormalizer.normalize(email);
    firstName = Texts.trim(firstName);
    lastName = Texts.trim(lastName);
  }
}
