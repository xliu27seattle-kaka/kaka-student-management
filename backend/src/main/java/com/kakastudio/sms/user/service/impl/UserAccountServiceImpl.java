package com.kakastudio.sms.user.service.impl;

import com.kakastudio.sms.common.error.ConflictException;
import com.kakastudio.sms.common.error.ErrorCode;
import com.kakastudio.sms.user.EmailTakenException;
import com.kakastudio.sms.user.domain.User;
import com.kakastudio.sms.user.domain.UserStatus;
import com.kakastudio.sms.user.repository.UserRepository;
import com.kakastudio.sms.user.service.NewAccount;
import com.kakastudio.sms.user.service.UserAccountService;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccountServiceImpl implements UserAccountService {

  private final UserRepository users;
  private final PasswordEncoder passwordEncoder;

  public UserAccountServiceImpl(UserRepository users, PasswordEncoder passwordEncoder) {
    this.users = users;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<User> findByEmail(String email) {
    return users.findByEmail(email);
  }

  @Override
  @Transactional
  public User create(NewAccount account) {
    if (users.findByEmail(account.email()).isPresent()) {
      throw new EmailTakenException();
    }
    User user = new User();
    user.setEmail(account.email());
    user.setFirstName(account.firstName());
    user.setLastName(account.lastName());
    user.setPasswordHash(passwordEncoder.encode(account.rawPassword()));
    user.setRole(account.role());
    user.setStatus(account.status());
    return users.save(user);
  }

  @Override
  @Transactional
  public void changeDetails(User user, String email, String firstName, String lastName) {
    boolean emailChanges = !user.getEmail().equals(email);
    if (emailChanges && users.existsByEmail(email)) {
      throw new EmailTakenException();
    }
    user.setEmail(email);
    user.setFirstName(firstName);
    user.setLastName(lastName);
  }

  @Override
  @Transactional
  public void changePassword(User user, String rawPassword) {
    user.setPasswordHash(passwordEncoder.encode(rawPassword));
  }

  @Override
  @Transactional
  public void changeStatus(User user, UserStatus from, UserStatus to) {
    if (user.getStatus() != from) {
      throw new ConflictException(
          ErrorCode.INVALID_STATUS_TRANSITION,
          "This account is " + user.getStatus() + "; only a " + from + " account can become " + to);
    }
    user.setStatus(to);
  }

  @Override
  @Transactional
  public void delete(User user) {
    users.delete(user);
  }
}
