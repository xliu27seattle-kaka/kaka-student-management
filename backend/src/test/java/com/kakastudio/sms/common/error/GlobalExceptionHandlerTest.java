package com.kakastudio.sms.common.error;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(GlobalExceptionHandlerTest.ProbeController.class)
@Import(GlobalExceptionHandlerTest.ProbeController.class)
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest {

  @Autowired private MockMvc mvc;

  @Test
  void apiExceptionKeepsStatusAndCode() throws Exception {
    mvc.perform(get("/probe/conflict"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"))
        .andExpect(jsonPath("$.message").value("Email already registered"))
        .andExpect(jsonPath("$.timestamp", notNullValue()));
  }

  @Test
  void validationListsFieldErrors() throws Exception {
    mvc.perform(
            post("/probe/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
  }

  @Test
  void malformedJsonIs400() throws Exception {
    mvc.perform(post("/probe/validate").contentType(MediaType.APPLICATION_JSON).content("{"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
  }

  @Test
  void unexpectedErrorHidesDetails() throws Exception {
    mvc.perform(get("/probe/boom"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
        .andExpect(jsonPath("$.message").value("Unexpected error"));
  }

  @Test
  void unknownPathIs404WithErrorBody() throws Exception {
    mvc.perform(get("/probe/nothing-here"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("REQUEST_FAILED"))
        .andExpect(jsonPath("$.message", notNullValue()));
  }

  @Test
  void aValueOfTheWrongTypeIs400NotACrash() throws Exception {
    mvc.perform(get("/probe/number/abc"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
        .andExpect(jsonPath("$.message").value("The value of 'id' is not valid"));
  }

  @Test
  void twoRegistrationsRacingForOneEmailGetEmailTaken() throws Exception {
    mvc.perform(get("/probe/duplicate-email"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
  }

  @Test
  void anyOtherBrokenConstraintIsAConflictWithoutDatabaseDetails() throws Exception {
    mvc.perform(get("/probe/duplicate-other"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("DATA_CONFLICT"))
        .andExpect(jsonPath("$.message").value("The request conflicts with existing data"));
  }

  @RestController
  static class ProbeController {

    @GetMapping("/probe/number/{id}")
    void number(@PathVariable Long id) {}

    @GetMapping("/probe/duplicate-email")
    void duplicateEmail() {
      throw new DataIntegrityViolationException(
          "could not execute statement",
          new RuntimeException(
              "ERROR: duplicate key value violates unique constraint \"users_email_key\""));
    }

    @GetMapping("/probe/duplicate-other")
    void duplicateOther() {
      throw new DataIntegrityViolationException(
          "could not execute statement",
          new RuntimeException(
              "ERROR: duplicate key value violates unique constraint \"students_student_no_key\""));
    }

    @GetMapping("/probe/conflict")
    void conflict() {
      throw new ConflictException(ErrorCode.EMAIL_TAKEN, "Email already registered");
    }

    @PostMapping("/probe/validate")
    void validate(@Valid @RequestBody Body body) {}

    @GetMapping("/probe/boom")
    void boom() {
      throw new IllegalStateException("db password is hunter2");
    }
  }

  record Body(@NotBlank String name) {}
}
