package com.kakastudio.notification.email.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** One email to send. {@code html} is the complete HTML body. */
public record EmailRequest(
    @NotBlank @Email String to, @NotBlank @Size(max = 200) String subject, @NotBlank String html) {}
