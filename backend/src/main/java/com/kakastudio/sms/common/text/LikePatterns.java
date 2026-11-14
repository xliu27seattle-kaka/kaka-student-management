package com.kakastudio.sms.common.text;

import java.util.Locale;

/** Builds the pattern of a case-insensitive "contains" search. */
public final class LikePatterns {

  /** The escape character to pass to the LIKE predicate. */
  public static final char ESCAPE = '!';

  private LikePatterns() {}

  /**
   * {@code %text%} in lower case, with the wildcards inside the text escaped, so a search for "50%"
   * or "a_b" looks for those characters.
   */
  public static String contains(String text) {
    String escaped =
        text.trim()
            .toLowerCase(Locale.ROOT)
            .replace("!", "!!")
            .replace("%", "!%")
            .replace("_", "!_");
    return "%" + escaped + "%";
  }
}
