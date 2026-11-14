package com.kakastudio.sms.notification.email.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.kakastudio.sms.notification.email.dto.EmailMessage;
import com.kakastudio.sms.notification.email.dto.EmailResult;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class RestTemplateEmailGatewayTest {

  private static final EmailMessage MESSAGE =
      new EmailMessage("lena@uni.example", "Hello", "<p>Hi</p>");

  private MockRestServiceServer server;
  private RestTemplateEmailGateway gateway;

  @BeforeEach
  void setUp() {
    RestTemplate restTemplate =
        new RestTemplateBuilder()
            .rootUri("http://notify.test")
            .defaultHeader("X-Internal-Key", "test-key")
            .build();
    server = MockRestServiceServer.bindTo(restTemplate).build();
    gateway = new RestTemplateEmailGateway(restTemplate);
  }

  @Test
  void postsTheMessageWithTheInternalKey() {
    server
        .expect(requestTo("http://notify.test/api/v1/emails"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("X-Internal-Key", "test-key"))
        .andExpect(jsonPath("$.to").value("lena@uni.example"))
        .andExpect(jsonPath("$.subject").value("Hello"))
        .andExpect(jsonPath("$.html").value("<p>Hi</p>"))
        .andRespond(
            withStatus(HttpStatus.ACCEPTED)
                .body("{\"status\":\"SENT\"}")
                .contentType(MediaType.APPLICATION_JSON));

    EmailResult result = gateway.send(MESSAGE);

    assertThat(result.sent()).isTrue();
    server.verify();
  }

  @Test
  void serverErrorIsATransientFailure() {
    server
        .expect(requestTo("http://notify.test/api/v1/emails"))
        .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

    EmailResult result = gateway.send(MESSAGE);

    assertThat(result.sent()).isFalse();
    assertThat(result.permanent()).isFalse();
    assertThat(result.error()).isNotBlank();
  }

  @Test
  void rejectedMailIsAPermanentFailure() {
    server
        .expect(requestTo("http://notify.test/api/v1/emails"))
        .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY));

    EmailResult result = gateway.send(MESSAGE);

    assertThat(result.sent()).isFalse();
    assertThat(result.permanent()).isTrue();
  }

  @Test
  void invalidRequestIsAPermanentFailure() {
    server
        .expect(requestTo("http://notify.test/api/v1/emails"))
        .andRespond(withStatus(HttpStatus.BAD_REQUEST));

    assertThat(gateway.send(MESSAGE).permanent()).isTrue();
  }

  @Test
  void connectionFailureIsATransientFailureAndDoesNotThrow() {
    server
        .expect(requestTo("http://notify.test/api/v1/emails"))
        .andRespond(withException(new IOException("connection refused")));

    EmailResult result = gateway.send(MESSAGE);

    assertThat(result.sent()).isFalse();
    assertThat(result.permanent()).isFalse();
  }
}
