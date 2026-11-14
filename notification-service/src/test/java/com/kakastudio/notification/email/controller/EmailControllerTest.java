package com.kakastudio.notification.email.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kakastudio.notification.common.MailDeliveryException;
import com.kakastudio.notification.config.AppProperties;
import com.kakastudio.notification.email.dto.EmailRequest;
import com.kakastudio.notification.email.service.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(EmailController.class)
@EnableConfigurationProperties(AppProperties.class)
@TestPropertySource(properties = "app.internal-api-key=test-key")
class EmailControllerTest {

  private static final String VALID_BODY =
      """
      {"to":"lena@uni.example","subject":"Welcome","html":"<p>Hi</p>"}
      """;

  @Autowired private MockMvc mvc;
  @MockitoBean private EmailService emailService;

  private MockHttpServletRequestBuilder request(String body) {
    return post("/api/v1/emails").contentType(MediaType.APPLICATION_JSON).content(body);
  }

  @Test
  void returns202WithValidKey() throws Exception {
    mvc.perform(request(VALID_BODY).header("X-Internal-Key", "test-key"))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.status").value("SENT"));

    verify(emailService).send(new EmailRequest("lena@uni.example", "Welcome", "<p>Hi</p>"));
  }

  @Test
  void returns401WithoutKey() throws Exception {
    mvc.perform(request(VALID_BODY))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INVALID_INTERNAL_KEY"));

    verify(emailService, never()).send(any());
  }

  @Test
  void returns401WithWrongKey() throws Exception {
    mvc.perform(request(VALID_BODY).header("X-Internal-Key", "nope"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INVALID_INTERNAL_KEY"));

    verify(emailService, never()).send(any());
  }

  @Test
  void returns400ForInvalidBody() throws Exception {
    String body =
        """
        {"to":"not-an-email","subject":"","html":"<p>Hi</p>"}
        """;

    mvc.perform(request(body).header("X-Internal-Key", "test-key"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors[*].field", containsInAnyOrder("to", "subject")));

    verify(emailService, never()).send(any());
  }

  @Test
  void returns400ForMalformedJson() throws Exception {
    mvc.perform(request("{").header("X-Internal-Key", "test-key"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
  }

  @Test
  void returns502WhenDeliveryFails() throws Exception {
    doThrow(new MailDeliveryException("smtp down", false, null)).when(emailService).send(any());

    mvc.perform(request(VALID_BODY).header("X-Internal-Key", "test-key"))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.code").value("MAIL_DELIVERY_FAILED"));
  }

  @Test
  void returns422WhenMailIsRejectedForGood() throws Exception {
    doThrow(new MailDeliveryException("no such user", true, null)).when(emailService).send(any());

    mvc.perform(request(VALID_BODY).header("X-Internal-Key", "test-key"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("MAIL_REJECTED"));
  }
}
