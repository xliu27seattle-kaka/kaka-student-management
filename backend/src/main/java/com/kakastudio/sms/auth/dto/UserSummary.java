package com.kakastudio.sms.auth.dto;

import com.kakastudio.sms.user.domain.Role;
import com.kakastudio.sms.user.domain.User;

/** What the frontend needs to know about the signed-in user. */
public record UserSummary(Long id, String email, String firstName, String lastName, Role role) {

  public static UserSummary of(User user) {
    return new UserSummary(
        user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getRole());
  }
}
