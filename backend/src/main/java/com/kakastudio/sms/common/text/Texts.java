package com.kakastudio.sms.common.text;

public final class Texts {

  private Texts() {}

  /** Trims, keeping {@code null} as {@code null} so validation can still report "required". */
  public static String trim(String value) {
    return value == null ? null : value.trim();
  }
}
