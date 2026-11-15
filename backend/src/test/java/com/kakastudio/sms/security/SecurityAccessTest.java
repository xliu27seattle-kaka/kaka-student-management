package com.kakastudio.sms.security;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kakastudio.sms.security.service.impl.JwtTokenService;
import com.kakastudio.sms.testsupport.Users;
import com.kakastudio.sms.user.domain.Role;
import com.kakastudio.sms.user.domain.User;
import com.kakastudio.sms.user.domain.UserStatus;
import com.kakastudio.sms.user.repository.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(SecurityAccessTest.PingController.class)
@Import({SecurityTestConfig.class, SecurityAccessTest.PingController.class})
@EnableConfigurationProperties(JwtProperties.class)
class SecurityAccessTest {

  @Autowired private MockMvc mvc;
  @Autowired private JwtTokenService tokenService;
  @Autowired private JwtEncoder jwtEncoder;
  @Autowired private JwtProperties jwtProperties;
  @MockitoBean private UserRepository users;

  private String tokenFor(User user) {
    when(users.findById(user.getId())).thenReturn(Optional.of(user));
    return "Bearer " + tokenService.issue(user).value();
  }

  @Test
  void noTokenIs401Json() throws Exception {
    mvc.perform(get("/api/v1/admin/ping"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
  }

  @Test
  void garbageTokenIs401Json() throws Exception {
    mvc.perform(get("/api/v1/admin/ping").header("Authorization", "Bearer not-a-jwt"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
  }

  @Test
  void expiredTokenIs401Json() throws Exception {
    User admin = Users.activeAdmin(1L);
    when(users.findById(1L)).thenReturn(Optional.of(admin));
    Clock twoDaysAgo = Clock.fixed(Instant.now().minus(Duration.ofDays(2)), ZoneOffset.UTC);
    String expired =
        new JwtTokenService(jwtEncoder, jwtProperties, twoDaysAgo).issue(admin).value();

    mvc.perform(get("/api/v1/admin/ping").header("Authorization", "Bearer " + expired))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
  }

  @Test
  void studentOnAdminPathIs403() throws Exception {
    mvc.perform(
            get("/api/v1/admin/ping").header("Authorization", tokenFor(Users.activeStudent(7L))))
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
  }

  @Test
  void adminOnAdminPathIs200() throws Exception {
    mvc.perform(get("/api/v1/admin/ping").header("Authorization", tokenFor(Users.activeAdmin(1L))))
        .andExpect(status().isOk());
  }

  @Test
  void studentOnStudentPathIs200() throws Exception {
    mvc.perform(
            get("/api/v1/student/ping").header("Authorization", tokenFor(Users.activeStudent(7L))))
        .andExpect(status().isOk());
  }

  @Test
  void deactivatedUserTokenIsRejected() throws Exception {
    User student = Users.activeStudent(7L);
    String token = tokenFor(student);
    student.setStatus(UserStatus.INACTIVE); // deactivated after the token was issued

    mvc.perform(get("/api/v1/student/ping").header("Authorization", token))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("SESSION_REVOKED"));
  }

  @Test
  void tokenOfDeletedUserIsRejected() throws Exception {
    String token = tokenFor(Users.user(9L, "gone@uni.example", Role.STUDENT, UserStatus.ACTIVE));
    when(users.findById(9L)).thenReturn(Optional.empty());

    mvc.perform(get("/api/v1/student/ping").header("Authorization", token))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("SESSION_REVOKED"));
  }

  /** Each area admits exactly its own role; the shared paths admit every signed-in role. */
  @ParameterizedTest(name = "{0} on {1} -> {2}")
  @CsvSource({
    "ADMIN, admin, 200",
    "ADMIN, professor, 403",
    "ADMIN, student, 403",
    "ADMIN, notifications, 200",
    "PROFESSOR, admin, 403",
    "PROFESSOR, professor, 200",
    "PROFESSOR, student, 403",
    "PROFESSOR, notifications, 200",
    "STUDENT, admin, 403",
    "STUDENT, professor, 403",
    "STUDENT, student, 200",
    "STUDENT, notifications, 200"
  })
  void eachRoleEntersOnlyItsOwnArea(Role role, String area, int expected) throws Exception {
    User user = Users.user(40L, "someone@kaka.example", role, UserStatus.ACTIVE);
    mvc.perform(get("/api/v1/" + area + "/ping").header("Authorization", tokenFor(user)))
        .andExpect(status().is(expected));
  }

  @RestController
  static class PingController {

    @GetMapping("/api/v1/admin/ping")
    String admin() {
      return "pong";
    }

    @GetMapping("/api/v1/student/ping")
    String student() {
      return "pong";
    }

    @GetMapping("/api/v1/professor/ping")
    String professor() {
      return "pong";
    }

    @GetMapping("/api/v1/notifications/ping")
    String anySignedIn() {
      return "pong";
    }
  }
}
