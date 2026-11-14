package com.kakastudio.sms.common.text;

import java.util.Locale;

/** Email addresses are compared and stored trimmed and in lower case. */
public final class EmailNormalizer {

  private EmailNormalizer() {}

  public static String normalize(String email) {
    return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
  }
}
