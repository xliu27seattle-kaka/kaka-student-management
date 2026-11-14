package com.kakastudio.sms.user.service.impl;

import com.kakastudio.sms.user.service.TemporaryPasswordGenerator;
import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class SecureTemporaryPasswordGenerator implements TemporaryPasswordGenerator {

  // No l, I, O, 0 or 1: a temporary password is read from an email and typed by hand.
  private static final String LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
  private static final String DIGITS = "23456789";
  private static final String ALL = LETTERS + DIGITS;
  private static final int LENGTH = 12;

  private final SecureRandom random = new SecureRandom();

  @Override
  public String generate() {
    char[] password = new char[LENGTH];
    for (int i = 0; i < LENGTH; i++) {
      password[i] = pick(ALL);
    }
    // Guarantee one letter and one digit, at two different random positions.
    int letterAt = random.nextInt(LENGTH);
    int digitAt = (letterAt + 1 + random.nextInt(LENGTH - 1)) % LENGTH;
    password[letterAt] = pick(LETTERS);
    password[digitAt] = pick(DIGITS);
    return new String(password);
  }

  private char pick(String alphabet) {
    return alphabet.charAt(random.nextInt(alphabet.length()));
  }
}
