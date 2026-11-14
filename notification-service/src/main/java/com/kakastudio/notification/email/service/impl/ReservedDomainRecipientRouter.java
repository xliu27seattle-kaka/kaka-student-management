package com.kakastudio.notification.email.service.impl;

import com.kakastudio.notification.email.service.MailRoute;
import com.kakastudio.notification.email.service.RecipientRouter;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Sends addresses on reserved test domains (RFC 2606) to the local inbox. Demo accounts use such
 * addresses, so they never produce real mail or bounces, even when real delivery is switched on.
 */
@Component
public class ReservedDomainRecipientRouter implements RecipientRouter {

  /** Top-level domains that can never exist. */
  private static final List<String> RESERVED_TOP_LEVEL =
      List.of("example", "test", "invalid", "localhost");

  /** Domains set aside for documentation; nobody receives mail there. */
  private static final List<String> RESERVED_DOMAINS =
      List.of("example.com", "example.net", "example.org");

  @Override
  public MailRoute route(String recipient) {
    String domain = recipient.substring(recipient.lastIndexOf('@') + 1).toLowerCase(Locale.ROOT);
    String topLevel = domain.substring(domain.lastIndexOf('.') + 1);
    boolean reserved =
        RESERVED_TOP_LEVEL.contains(topLevel)
            || RESERVED_DOMAINS.stream()
                .anyMatch(name -> domain.equals(name) || domain.endsWith("." + name));
    return reserved ? MailRoute.SANDBOX : MailRoute.PRIMARY;
  }
}
