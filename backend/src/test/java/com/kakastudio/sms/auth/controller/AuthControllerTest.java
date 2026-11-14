package com.kakastudio.sms.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kakastudio.sms.auth.dto.LoginRequest;
import com.kakastudio.sms.auth.dto.LoginResponse;
import com.kakastudio.sms.auth.dto.UserSummary;
import com.kakastudio.sms.auth.service.AuthService;
import com.kakastudio.sms.security.JwtProperties;
import com.kakastudio.sms.security.SecurityTestConfig;
import com.kakastudio.sms.student.service.StudentService;
import com.kakastudio.sms.user.domain.Role;
import com.kakastudio.sms.user.repository.UserRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import(SecurityTestConfig.class)
@EnableConfigurationProperties(JwtProperties.class)
class AuthControllerTest {

  @Autowired private MockMvc mvc;
  @MockitoBean private AuthService authService;
  @MockitoBean private StudentService studentService;
  @MockitoBean private UserRepository users;

  private static String json(String email, String password) {
    return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
  }

  @Test
  void loginIsPublicAndReturnsTheToken() throws Exception {
    when(authService.login(any()))
        .thenReturn(
            new LoginResponse(
                "jwt-value",
                Instant.parse("2026-11-13T12:00:00Z"),
                new UserSummary(1L, "admin@kaka.example", "Kaka", "Admin", Role.ADMIN)));

    mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("admin@kaka.example", "KakaDemo2026")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("jwt-value"))
        .andExpect(jsonPath("$.user.role").value("ADMIN"));
  }

  @Test
  void loginAcceptsAnEmailTypedWithSpacesAndCapitals() throws Exception {
    mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("  Admin@Kaka.Example ", "KakaDemo2026")))
        .andExpect(status().isOk());

    verify(authService).login(new LoginRequest("admin@kaka.example", "KakaDemo2026"));
  }

  @Test
  void loginRejectsSomethingThatIsNotAnEmail() throws Exception {
    mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("not-an-email", "x")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("email"));
  }
}
