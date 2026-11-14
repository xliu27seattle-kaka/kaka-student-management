package com.kakastudio.sms.auth.controller;

import com.kakastudio.sms.auth.dto.ChangePasswordRequest;
import com.kakastudio.sms.auth.dto.LoginRequest;
import com.kakastudio.sms.auth.dto.LoginResponse;
import com.kakastudio.sms.auth.dto.UserSummary;
import com.kakastudio.sms.auth.service.AuthService;
import com.kakastudio.sms.student.dto.RegisterRequest;
import com.kakastudio.sms.student.dto.RegisterResponse;
import com.kakastudio.sms.student.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthService authService;
  private final StudentService studentService;

  public AuthController(AuthService authService, StudentService studentService) {
    this.authService = authService;
    this.studentService = studentService;
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
    return studentService.register(request);
  }

  @PostMapping("/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request);
  }

  @GetMapping("/me")
  public UserSummary me() {
    return authService.me();
  }

  @PutMapping("/password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
    authService.changePassword(request);
  }
}
