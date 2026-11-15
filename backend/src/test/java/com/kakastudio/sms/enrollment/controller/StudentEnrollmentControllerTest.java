package com.kakastudio.sms.enrollment.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kakastudio.sms.common.error.ErrorCode;
import com.kakastudio.sms.common.error.RuleViolationException;
import com.kakastudio.sms.enrollment.service.EnrollmentService;
import com.kakastudio.sms.security.JwtProperties;
import com.kakastudio.sms.security.SecurityTestConfig;
import com.kakastudio.sms.security.service.impl.JwtTokenService;
import com.kakastudio.sms.student.service.CurrentStudent;
import com.kakastudio.sms.testsupport.Users;
import com.kakastudio.sms.user.domain.User;
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

@WebMvcTest(StudentEnrollmentController.class)
@Import(SecurityTestConfig.class)
@EnableConfigurationProperties(JwtProperties.class)
class StudentEnrollmentControllerTest {

  @Autowired private MockMvc mvc;
  @Autowired private JwtTokenService tokenService;
  @MockitoBean private EnrollmentService enrollmentService;
  @MockitoBean private CurrentStudent currentStudent;
  @MockitoBean private UserRepository users;

  private String tokenFor(User user) {
    when(users.findById(user.getId())).thenReturn(Optional.of(user));
    return "Bearer " + tokenService.issue(user).value();
  }

  @Test
  void aSectionIsRequired() throws Exception {
    mvc.perform(
            post("/api/v1/student/enrollments")
                .header("Authorization", tokenFor(Users.activeStudent(7L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("sectionId"));
    verifyNoInteractions(enrollmentService);
  }

  @Test
  void aRefusalCarriesTheReason() throws Exception {
    when(currentStudent.id()).thenReturn(3L);
    when(enrollmentService.enroll(3L, 42L))
        .thenThrow(new RuleViolationException(ErrorCode.NOT_ELIGIBLE, "Section full"));

    mvc.perform(
            post("/api/v1/student/enrollments")
                .header("Authorization", tokenFor(Users.activeStudent(7L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sectionId\":42}"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("NOT_ELIGIBLE"))
        .andExpect(jsonPath("$.message").value("Section full"));
  }
}
