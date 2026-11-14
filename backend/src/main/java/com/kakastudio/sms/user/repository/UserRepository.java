package com.kakastudio.sms.user.repository;

import com.kakastudio.sms.user.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  /** {@code email} must already be normalized. */
  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);
}
