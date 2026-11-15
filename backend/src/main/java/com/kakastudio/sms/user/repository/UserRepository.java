package com.kakastudio.sms.user.repository;

import com.kakastudio.sms.user.domain.Role;
import com.kakastudio.sms.user.domain.User;
import com.kakastudio.sms.user.domain.UserStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  /** {@code email} must already be normalized. */
  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  /** For notifications: everyone with this role and status, by name. */
  List<User> findByRoleAndStatus(Role role, UserStatus status);

  long countByRoleAndStatus(Role role, UserStatus status);
}
