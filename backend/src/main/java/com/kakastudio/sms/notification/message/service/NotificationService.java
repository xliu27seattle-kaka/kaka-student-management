package com.kakastudio.sms.notification.message.service;

import com.kakastudio.sms.common.paging.PageResponse;
import com.kakastudio.sms.notification.message.dto.AdminNotificationRequest;
import com.kakastudio.sms.notification.message.dto.InboxItem;
import com.kakastudio.sms.notification.message.dto.NotificationRequest;
import com.kakastudio.sms.notification.message.dto.SentNotification;
import java.util.List;

/**
 * Writing and reading notifications. Every recipient gets a row in their inbox and an email; the
 * email goes through the outbox, so a failed send never undoes the notification.
 */
public interface NotificationService {

  SentNotification sendAsAdmin(Long senderUserId, AdminNotificationRequest request);

  /** To the students currently enrolled in one of the professor's own sections. */
  SentNotification sendToSection(Long professorUserId, Long sectionId, NotificationRequest request);

  List<SentNotification> sentBy(Long senderUserId);

  PageResponse<InboxItem> inbox(Long userId, int page, int size);

  long unreadCount(Long userId);

  void markRead(Long userId, Long recipientId);

  void markAllRead(Long userId);
}
