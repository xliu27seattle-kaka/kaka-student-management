package com.kakastudio.sms.notification.message.controller;

import com.kakastudio.sms.common.paging.PageResponse;
import com.kakastudio.sms.notification.message.dto.AdminNotificationRequest;
import com.kakastudio.sms.notification.message.dto.InboxItem;
import com.kakastudio.sms.notification.message.dto.NotificationRequest;
import com.kakastudio.sms.notification.message.dto.SentNotification;
import com.kakastudio.sms.notification.message.dto.UnreadCount;
import com.kakastudio.sms.notification.message.service.NotificationService;
import com.kakastudio.sms.security.service.CurrentUserProvider;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The inbox is for every signed-in user; writing is for administrators and for professors (to their
 * own sections). The path prefix decides the role.
 */
@RestController
public class NotificationController {

  private final NotificationService notificationService;
  private final CurrentUserProvider currentUser;

  public NotificationController(
      NotificationService notificationService, CurrentUserProvider currentUser) {
    this.notificationService = notificationService;
    this.currentUser = currentUser;
  }

  @GetMapping("/api/v1/notifications")
  public PageResponse<InboxItem> inbox(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return notificationService.inbox(currentUser.require().id(), page, size);
  }

  @GetMapping("/api/v1/notifications/unread-count")
  public UnreadCount unreadCount() {
    return new UnreadCount(notificationService.unreadCount(currentUser.require().id()));
  }

  @PostMapping("/api/v1/notifications/{id}/read")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void markRead(@PathVariable Long id) {
    notificationService.markRead(currentUser.require().id(), id);
  }

  @PostMapping("/api/v1/notifications/read-all")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void markAllRead() {
    notificationService.markAllRead(currentUser.require().id());
  }

  @PostMapping("/api/v1/admin/notifications")
  @ResponseStatus(HttpStatus.CREATED)
  public SentNotification sendAsAdmin(@Valid @RequestBody AdminNotificationRequest request) {
    return notificationService.sendAsAdmin(currentUser.require().id(), request);
  }

  @GetMapping("/api/v1/admin/notifications")
  public List<SentNotification> sentByAdmin() {
    return notificationService.sentBy(currentUser.require().id());
  }

  @PostMapping("/api/v1/professor/sections/{sectionId}/notifications")
  @ResponseStatus(HttpStatus.CREATED)
  public SentNotification sendToSection(
      @PathVariable Long sectionId, @Valid @RequestBody NotificationRequest request) {
    return notificationService.sendToSection(currentUser.require().id(), sectionId, request);
  }

  @GetMapping("/api/v1/professor/notifications")
  public List<SentNotification> sentByProfessor() {
    return notificationService.sentBy(currentUser.require().id());
  }
}
