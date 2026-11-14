package com.kakastudio.notification.email.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.kakastudio.notification.email.service.MailRoute;
import org.junit.jupiter.api.Test;

class ReservedDomainRecipientRouterTest {

  private final ReservedDomainRecipientRouter router = new ReservedDomainRecipientRouter();

  @Test
  void exampleDomainGoesToSandbox() {
    assertThat(router.route("lena@uni.example")).isEqualTo(MailRoute.SANDBOX);
  }

  @Test
  void subdomainOfTestDomainGoesToSandbox() {
    assertThat(router.route("x@sub.domain.test")).isEqualTo(MailRoute.SANDBOX);
  }

  @Test
  void upperCaseReservedDomainGoesToSandbox() {
    assertThat(router.route("A@UNI.EXAMPLE")).isEqualTo(MailRoute.SANDBOX);
  }

  @Test
  void invalidAndLocalhostDomainsGoToSandbox() {
    assertThat(router.route("a@nowhere.invalid")).isEqualTo(MailRoute.SANDBOX);
    assertThat(router.route("a@box.localhost")).isEqualTo(MailRoute.SANDBOX);
  }

  @Test
  void realDomainGoesToPrimary() {
    assertThat(router.route("someone@gmail.com")).isEqualTo(MailRoute.PRIMARY);
  }

  @Test
  void theReservedExampleDomainsGoToSandbox() {
    assertThat(router.route("someone@example.com")).isEqualTo(MailRoute.SANDBOX);
    assertThat(router.route("someone@example.org")).isEqualTo(MailRoute.SANDBOX);
    assertThat(router.route("someone@example.net")).isEqualTo(MailRoute.SANDBOX);
    assertThat(router.route("someone@mail.example.com")).isEqualTo(MailRoute.SANDBOX);
  }

  @Test
  void aDomainThatOnlyLooksReservedGoesToPrimary() {
    assertThat(router.route("someone@notexample.com")).isEqualTo(MailRoute.PRIMARY);
    assertThat(router.route("someone@example.com.au")).isEqualTo(MailRoute.PRIMARY);
  }
}
