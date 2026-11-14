package com.kakastudio.sms.student.controller;

import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kakastudio.sms.auth.controller.AuthController;
import com.kakastudio.sms.auth.service.AuthService;
import com.kakastudio.sms.security.JwtProperties;
import com.kakastudio.sms.security.SecurityTestConfig;
import com.kakastudio.sms.security.service.impl.JwtTokenService;
import com.kakastudio.sms.student.domain.AcademicStatus;
import com.kakastudio.sms.student.dto.RegisterResponse;
import com.kakastudio.sms.student.dto.StudentResponse;
import com.kakastudio.sms.student.service.StudentService;
import com.kakastudio.sms.testsupport.Users;
import com.kakastudio.sms.user.domain.User;
import com.kakastudio.sms.user.domain.UserStatus;
import com.kakastudio.sms.user.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({AdminStudentController.class, AuthController.class})
@Import(SecurityTestConfig.class)
@EnableConfigurationProperties(JwtProperties.class)
class AdminStudentControllerTest {

  @Autowired private MockMvc mvc;
  @Autowired private JwtTokenService tokenService;
  @MockitoBean private StudentService studentService;
  @MockitoBean private AuthService authService;
  @MockitoBean private UserRepository users;

  private String tokenFor(User user) {
    when(users.findById(user.getId())).thenReturn(Optional.of(user));
    return "Bearer " + tokenService.issue(user).value();
  }

  @Test
  void listIsForAdministratorsOnly() throws Exception {
    mvc.perform(get("/api/v1/admin/students"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    mvc.perform(
            get("/api/v1/admin/students")
                .header("Authorization", tokenFor(Users.activeStudent(7L))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
  }

  @Test
  void registerIsPublicAndReturns201() throws Exception {
    when(studentService.register(any()))
        .thenReturn(new RegisterResponse("S2026-0001", UserStatus.PENDING));

    mvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"lena.fischer@uni.example","firstName":"Lena","lastName":"Fischer","password":"Passw0rd1"}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.studentNo").value("S2026-0001"))
        .andExpect(jsonPath("$.status").value("PENDING"));
  }

  @Test
  void registerListsEveryInvalidField() throws Exception {
    mvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"x","firstName":"","lastName":"F","password":"short"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors[*].field", hasItems("email", "firstName", "password")));
  }

  @Test
  void approveReturnsTheStudent() throws Exception {
    when(studentService.approve(3L))
        .thenReturn(
            new StudentResponse(
                3L,
                "S2026-0001",
                "lena.fischer@uni.example",
                "Lena",
                "Fischer",
                UserStatus.ACTIVE,
                AcademicStatus.ENROLLED,
                Instant.parse("2026-11-13T10:00:00Z")));

    mvc.perform(
            post("/api/v1/admin/students/3/approve")
                .header("Authorization", tokenFor(Users.activeAdmin(1L))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.studentNo").value("S2026-0001"));
  }
}
