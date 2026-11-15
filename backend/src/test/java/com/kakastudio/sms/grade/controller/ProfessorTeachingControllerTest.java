package com.kakastudio.sms.grade.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kakastudio.sms.common.error.ErrorCode;
import com.kakastudio.sms.common.error.ForbiddenException;
import com.kakastudio.sms.grade.service.GradingService;
import com.kakastudio.sms.security.JwtProperties;
import com.kakastudio.sms.security.SecurityTestConfig;
import com.kakastudio.sms.security.service.impl.JwtTokenService;
import com.kakastudio.sms.security.service.impl.SecurityContextCurrentUserProvider;
import com.kakastudio.sms.testsupport.Users;
import com.kakastudio.sms.user.domain.Role;
import com.kakastudio.sms.user.domain.User;
import com.kakastudio.sms.user.domain.UserStatus;
import com.kakastudio.sms.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProfessorTeachingController.class)
@Import({SecurityTestConfig.class, SecurityContextCurrentUserProvider.class})
@EnableConfigurationProperties(JwtProperties.class)
class ProfessorTeachingControllerTest {

  @Autowired private MockMvc mvc;
  @Autowired private JwtTokenService tokenService;
  @MockitoBean private GradingService gradingService;
  @MockitoBean private UserRepository users;

  private final User professor =
      Users.user(21L, "amara.okafor@faculty.example", Role.PROFESSOR, UserStatus.ACTIVE);

  private String tokenFor(User user) {
    when(users.findById(user.getId())).thenReturn(Optional.of(user));
    return "Bearer " + tokenService.issue(user).value();
  }

  private org.springframework.test.web.servlet.ResultActions putGrades(User user, String body)
      throws Exception {
    return mvc.perform(
        put("/api/v1/professor/sections/10/grades")
            .header("Authorization", tokenFor(user))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  @Test
  void aScoreAbove100IsRefusedBeforeTheService() throws Exception {
    putGrades(professor, "{\"grades\":[{\"enrollmentId\":7,\"score\":101}]}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    verifyNoInteractions(gradingService);
  }

  @Test
  void gradesAreSavedForTheSignedInProfessor() throws Exception {
    putGrades(professor, "{\"grades\":[{\"enrollmentId\":7,\"score\":91}]}")
        .andExpect(status().isOk());
    verify(gradingService).grade(eq(21L), eq(10L), any());
  }

  @Test
  void anotherProfessorsSectionIs403() throws Exception {
    when(gradingService.grade(eq(21L), eq(10L), any()))
        .thenThrow(new ForbiddenException(ErrorCode.NOT_YOUR_SECTION, "Not yours"));

    putGrades(professor, "{\"grades\":[{\"enrollmentId\":7,\"score\":91}]}")
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("NOT_YOUR_SECTION"));
  }

  @Test
  void studentsCannotGrade() throws Exception {
    putGrades(Users.activeStudent(30L), "{\"grades\":[]}").andExpect(status().isForbidden());
    verifyNoInteractions(gradingService);
  }
}
