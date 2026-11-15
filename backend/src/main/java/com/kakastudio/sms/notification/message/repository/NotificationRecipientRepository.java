package com.kakastudio.sms.notification.message.repository;

import com.kakastudio.sms.notification.message.domain.NotificationRecipient;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRecipientRepository
    extends JpaRepository<NotificationRecipient, Long> {

  /** A user's inbox, newest first, with the notification and its sender loaded. */
  @Query(
      value =
          "select r from NotificationRecipient r join fetch r.notification n join fetch n.sender"
              + " left join fetch n.section s left join fetch s.course where r.user.id = :userId",
      countQuery = "select count(r) from NotificationRecipient r where r.user.id = :userId")
  Page<NotificationRecipient> findInbox(@Param("userId") Long userId, Pageable pageable);

  long countByUserIdAndReadAtIsNull(Long userId);

  @Modifying
  @Query(
      "update NotificationRecipient r set r.readAt = :now where r.user.id = :userId and r.readAt is null")
  int markAllRead(@Param("userId") Long userId, @Param("now") Instant now);

  /** Delivery status of the emails of the given notifications. */
  @Query(
      "select new com.kakastudio.sms.notification.message.repository.EmailStatusCount("
          + "r.notification.id, o.status, count(r))"
          + " from NotificationRecipient r, EmailOutbox o where o.id = r.emailOutboxId"
          + " and r.notification.id in :ids group by r.notification.id, o.status")
  List<EmailStatusCount> countEmailStatuses(@Param("ids") Collection<Long> ids);
}
