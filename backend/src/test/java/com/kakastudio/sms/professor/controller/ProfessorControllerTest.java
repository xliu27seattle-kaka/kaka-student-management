package com.kakastudio.sms.professor.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kakastudio.sms.professor.service.ProfessorAdminService;
import com.kakastudio.sms.professor.service.ProfessorDirectoryService;
import com.kakastudio.sms.professor.service.ProfessorProfileService;
import com.kakastudio.sms.section.service.SectionService;
import com.kakastudio.sms.security.JwtProperties;
import com.kakastudio.sms.security.SecurityTestConfig;
import com.kakastudio.sms.security.service.impl.JwtTokenService;
import com.kakastudio.sms.testsupport.Users;
import com.kakastudio.sms.user.domain.User;
import com.kakastudio.sms.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({
  AdminProfessorController.class,
  ProfessorDirectoryController.class,
  ProfessorSelfController.class
})
@Import(SecurityTestConfig.class)
@EnableConfigurationProperties(JwtProperties.class)
class ProfessorControllerTest {

  @Autowired private MockMvc mvc;
  @Autowired private JwtTokenService tokenService;
  @MockitoBean private ProfessorAdminService adminService;
  @MockitoBean private ProfessorDirectoryService directoryService;
  @MockitoBean private ProfessorProfileService profileService;
  @MockitoBean private UserRepository users;
  @MockitoBean private SectionService sectionService;

  private String tokenFor(User user) {
    when(users.findById(user.getId())).thenReturn(Optional.of(user));
    return "Bearer " + tokenService.issue(user).value();
  }

  @Test
  void aStudentCanReadTheDirectory() throws Exception {
    when(directoryService.directory()).thenReturn(List.of());

    mvc.perform(
            get("/api/v1/professors").header("Authorization", tokenFor(Users.activeStudent(7L))))
        .andExpect(status().isOk());
  }

  @Test
  void nobodyReadsTheDirectoryWithoutSigningIn() throws Exception {
    mvc.perform(get("/api/v1/professors"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
  }

  @Test
  void aStudentCannotEditAProfessorProfile() throws Exception {
    mvc.perform(
            put("/api/v1/professor/profile")
                .header("Authorization", tokenFor(Users.activeStudent(7L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
  }

  @Test
  void creatingAProfessorValidatesTheNestedProfile() throws Exception {
    mvc.perform(
            post("/api/v1/admin/professors")
                .header("Authorization", tokenFor(Users.activeAdmin(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"amara.okafor@faculty.example","firstName":"Amara","lastName":"Okafor",
                     "profile":{"awards":[{"title":"Too early","year":1500}]}}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("profile.awards[0].year")));
  }
}
