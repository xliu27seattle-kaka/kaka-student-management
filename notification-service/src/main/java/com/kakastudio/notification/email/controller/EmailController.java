package com.kakastudio.notification.email.controller;

import com.kakastudio.notification.email.dto.EmailRequest;
import com.kakastudio.notification.email.dto.EmailResponse;
import com.kakastudio.notification.email.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/emails")
public class EmailController {

  private final EmailService emailService;

  public EmailController(EmailService emailService) {
    this.emailService = emailService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.ACCEPTED)
  public EmailResponse send(@Valid @RequestBody EmailRequest request) {
    emailService.send(request);
    return EmailResponse.sent();
  }
}
