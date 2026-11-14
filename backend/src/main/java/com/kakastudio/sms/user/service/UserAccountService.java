package com.kakastudio.sms.user.service;

import com.kakastudio.sms.user.domain.User;
import com.kakastudio.sms.user.domain.UserStatus;
import java.util.Optional;

/**
 * The rules every account obeys, whoever it belongs to: a unique email, an encoded password, and
 * status changes that only go the allowed way. The student and professor services build on it.
 */
public interface UserAccountService {

  /** {@code email} must already be normalized. */
  Optional<User> findByEmail(String email);

  /**
   * @throws com.kakastudio.sms.user.EmailTakenException if the email belongs to another account
   */
  User create(NewAccount account);

  /**
   * @throws com.kakastudio.sms.user.EmailTakenException if the new email belongs to another account
   */
  void changeDetails(User user, String email, String firstName, String lastName);

  void changePassword(User user, String rawPassword);

  /**
   * Moves the account from one status to another. Repeating an action (approving twice) is a
   * conflict, not a crash.
   *
   * @throws com.kakastudio.sms.common.error.ConflictException if the account is not in {@code from}
   */
  void changeStatus(User user, UserStatus from, UserStatus to);

  void delete(User user);
}
