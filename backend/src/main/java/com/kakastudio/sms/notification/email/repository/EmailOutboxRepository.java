package com.kakastudio.sms.notification.email.repository;

import com.kakastudio.sms.notification.email.domain.EmailOutbox;
import com.kakastudio.sms.notification.email.domain.OutboxStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailOutboxRepository extends JpaRepository<EmailOutbox, Long> {

  long countByStatus(OutboxStatus status);

  /** The delivery log: optional status, optional LIKE pattern on recipient or subject. */
  @Query(
      "select o from EmailOutbox o where (:status is null or o.status = :status)"
          + " and (:pattern is null or lower(o.recipient) like :pattern escape '!'"
          + " or lower(o.subject) like :pattern escape '!')")
  Page<EmailOutbox> search(
      @Param("status") OutboxStatus status, @Param("pattern") String pattern, Pageable pageable);

  /**
   * Loads one row and locks it until the transaction ends, so the after-commit dispatch and the
   * background sweep can never send the same email twice.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select o from EmailOutbox o where o.id = :id")
  Optional<EmailOutbox> findWithLockById(@Param("id") Long id);

  @Query(
      "select o.id from EmailOutbox o"
          + " where o.status = :status and o.nextAttemptAt <= :now order by o.id")
  List<Long> findDueIds(
      @Param("status") OutboxStatus status, @Param("now") Instant now, Pageable pageable);
}
