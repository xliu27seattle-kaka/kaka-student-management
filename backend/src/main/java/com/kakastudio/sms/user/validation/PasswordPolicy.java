package com.kakastudio.sms.user.validation;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/** The one definition of an acceptable password, used by every request that sets one. */
public final class PasswordPolicy {

  public static final String MESSAGE =
      "Password must be at least 8 characters (at most 72 bytes) with at least one letter and one"
          + " digit";

  /** BCrypt only looks at the first 72 bytes of a password and refuses longer ones. */
  public static final int MAX_BYTES = 72;

  private static final int MIN_LENGTH = 8;
  private static final Pattern LETTER = Pattern.compile("[A-Za-z]");
  private static final Pattern DIGIT = Pattern.compile("\\d");

  private PasswordPolicy() {}

  public static boolean isAcceptable(String password) {
    return password != null
        && password.length() >= MIN_LENGTH
        && fitsBcrypt(password)
        && LETTER.matcher(password).find()
        && DIGIT.matcher(password).find();
  }

  /** Whether the password hasher can take this value at all. */
  public static boolean fitsBcrypt(String password) {
    return password != null && password.getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES;
  }
}
