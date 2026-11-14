package com.kakastudio.sms.auth.dto;

import com.kakastudio.sms.common.text.EmailNormalizer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {

  /** Normalizes the email before validation, so " Lena@Uni.Example " is accepted as typed. */
  public LoginRequest {
    email = EmailNormalizer.normalize(email);
  }
}
