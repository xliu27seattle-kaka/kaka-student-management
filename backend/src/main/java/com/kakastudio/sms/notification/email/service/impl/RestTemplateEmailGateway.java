package com.kakastudio.sms.notification.email.service.impl;

import com.kakastudio.sms.notification.email.dto.EmailMessage;
import com.kakastudio.sms.notification.email.dto.EmailResult;
import com.kakastudio.sms.notification.email.service.EmailGateway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/** Sends email by calling the notification service's REST endpoint. */
@Component
public class RestTemplateEmailGateway implements EmailGateway {

  private static final String EMAILS_PATH = "/api/v1/emails";

  private final RestTemplate restTemplate;

  public RestTemplateEmailGateway(
      @Qualifier("notificationRestTemplate") RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Override
  public EmailResult send(EmailMessage message) {
    try {
      restTemplate.postForEntity(EMAILS_PATH, message, Void.class);
      return EmailResult.ok();
    } catch (HttpClientErrorException ex) {
      // 400: the request itself is wrong. 422: the mail server refused the address for good.
      boolean permanent =
          ex.getStatusCode().isSameCodeAs(HttpStatus.BAD_REQUEST)
              || ex.getStatusCode().isSameCodeAs(HttpStatus.UNPROCESSABLE_ENTITY);
      String error = "Notification service answered " + ex.getStatusCode().value();
      return permanent ? EmailResult.permanentFailure(error) : EmailResult.transientFailure(error);
    } catch (RestClientException ex) {
      // Service down, timeout, 5xx: worth trying again later.
      return EmailResult.transientFailure(ex.getMessage());
    }
  }
}
