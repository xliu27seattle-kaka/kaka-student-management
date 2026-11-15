package com.kakastudio.sms.notification.message.repository;

import com.kakastudio.sms.notification.message.domain.Notification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

  @Query(
      "select n from Notification n left join fetch n.section s left join fetch s.course"
          + " where n.sender.id = :senderId order by n.createdAt desc")
  List<Notification> findSentBy(@Param("senderId") Long senderId);
}
