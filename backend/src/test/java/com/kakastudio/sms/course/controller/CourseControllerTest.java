package com.kakastudio.sms.course.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kakastudio.sms.common.error.ErrorCode;
import com.kakastudio.sms.common.error.RuleViolationException;
import com.kakastudio.sms.course.dto.CourseRequest;
import com.kakastudio.sms.course.service.CourseService;
import com.kakastudio.sms.security.JwtProperties;
import com.kakastudio.sms.security.SecurityTestConfig;
import com.kakastudio.sms.security.service.impl.JwtTokenService;
import com.kakastudio.sms.testsupport.Users;
import com.kakastudio.sms.user.domain.User;
import com.kakastudio.sms.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({CourseController.class, AdminCourseController.class})
@Import(SecurityTestConfig.class)
@EnableConfigurationProperties(JwtProperties.class)
class CourseControllerTest {

  @Autowired private MockMvc mvc;
  @Autowired private JwtTokenService tokenService;
  @MockitoBean private CourseService courseService;
  @MockitoBean private UserRepository users;

  private String tokenFor(User user) {
    when(users.findById(user.getId())).thenReturn(Optional.of(user));
    return "Bearer " + tokenService.issue(user).value();
  }

  @Test
  void studentsCanReadTheCatalogueButNotChangeIt() throws Exception {
    String student = tokenFor(Users.activeStudent(7L));
    mvc.perform(get("/api/v1/courses").header("Authorization", student)).andExpect(status().isOk());
    mvc.perform(
            post("/api/v1/admin/courses")
                .header("Authorization", student)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void normalisesTheCodeAndAppliesDefaults() throws Exception {
    mvc.perform(
            post("/api/v1/admin/courses")
                .header("Authorization", tokenFor(Users.activeAdmin(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"code":" cs 250 ","title":"Algorithms","department":"cs"}
                    """))
        .andExpect(status().isCreated());

    ArgumentCaptor<CourseRequest> request = ArgumentCaptor.forClass(CourseRequest.class);
    verify(courseService).create(request.capture());
    assertThat(request.getValue().code()).isEqualTo("CS250");
    assertThat(request.getValue().credits()).isEqualTo(3);
    assertThat(request.getValue().prerequisiteIds()).isEqualTo(List.of());
  }

  @Test
  void rejectsABadCodeAndTooManyCredits() throws Exception {
    mvc.perform(
            post("/api/v1/admin/courses")
                .header("Authorization", tokenFor(Users.activeAdmin(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"code":"Algorithms","title":"Algorithms","department":"CS","credits":9}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("code"))
        .andExpect(jsonPath("$.fieldErrors[1].field").value("credits"));
  }

  @Test
  void aCycleIsReportedAs422WithThePath() throws Exception {
    when(courseService.update(any(), any()))
        .thenThrow(
            new RuleViolationException(
                ErrorCode.PREREQUISITE_CYCLE,
                "These prerequisites would create a cycle: CS101 → CS301 → CS101"));

    mvc.perform(
            put("/api/v1/admin/courses/1")
                .header("Authorization", tokenFor(Users.activeAdmin(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"code":"CS101","title":"Intro","department":"CS","prerequisiteIds":[3]}
                    """))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("PREREQUISITE_CYCLE"))
        .andExpect(
            jsonPath("$.message")
                .value("These prerequisites would create a cycle: CS101 → CS301 → CS101"));
  }
}
