package com.kakastudio.sms.notification.email.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.kakastudio.sms.notification.email.dto.EmailMessage;
import com.kakastudio.sms.testsupport.Users;
import com.kakastudio.sms.user.domain.User;
import org.junit.jupiter.api.Test;

class HtmlEmailComposerTest {

  private final HtmlEmailComposer composer = new HtmlEmailComposer();

  @Test
  void registrationReceivedNamesTheStudentNumber() {
    EmailMessage message = composer.registrationReceived(Users.activeStudent(7L), "S2026-0001");

    assertThat(message.to()).isEqualTo("lena.fischer@uni.example");
    assertThat(message.subject()).isEqualTo("We received your registration");
    assertThat(message.html()).contains("Lena").contains("S2026-0001").contains("Kaka Studio");
  }

  @Test
  void markupInANameIsEscaped() {
    User user = Users.activeStudent(7L);
    user.setFirstName("<b>Ann</b>");

    String html = composer.registrationReceived(user, "S2026-0001").html();

    assertThat(html).contains("&lt;b&gt;Ann&lt;/b&gt;").doesNotContain("<b>Ann</b>");
  }

  @Test
  void accountCreatedCarriesTheTemporaryPassword() {
    EmailMessage message = composer.accountCreated(Users.activeStudent(7L), "Temp1234abcd");

    assertThat(message.subject()).isEqualTo("Your Kaka Student Management account");
    assertThat(message.html()).contains("Temp1234abcd").contains("lena.fischer@uni.example");
  }

  @Test
  void decisionEmailsHaveTheirSubjects() {
    User user = Users.activeStudent(7L);

    assertThat(composer.accountApproved(user).subject()).isEqualTo("Your account is approved");
    assertThat(composer.accountRejected(user).subject())
        .isEqualTo("Your registration was not approved");
  }
}
