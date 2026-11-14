package com.kakastudio.sms.testsupport;

import com.kakastudio.sms.user.domain.Role;
import com.kakastudio.sms.user.domain.User;
import com.kakastudio.sms.user.domain.UserStatus;

/** Builds users for tests. */
public final class Users {

  private Users() {}

  public static User user(Long id, String email, Role role, UserStatus status) {
    User user = new User();
    user.setId(id);
    user.setEmail(email);
    user.setPasswordHash("encoded:Passw0rd1");
    user.setFirstName("Lena");
    user.setLastName("Fischer");
    user.setRole(role);
    user.setStatus(status);
    return user;
  }

  public static User activeStudent(Long id) {
    return user(id, "lena.fischer@uni.example", Role.STUDENT, UserStatus.ACTIVE);
  }

  public static User activeAdmin(Long id) {
    return user(id, "admin@kaka.example", Role.ADMIN, UserStatus.ACTIVE);
  }
}
