package com.kakastudio.sms.notification.message.service.impl;

import com.kakastudio.sms.common.error.ApiException;
import com.kakastudio.sms.common.error.ErrorCode;
import com.kakastudio.sms.common.error.ForbiddenException;
import com.kakastudio.sms.common.error.NotFoundException;
import com.kakastudio.sms.common.error.RuleViolationException;
import com.kakastudio.sms.common.paging.PageRequests;
import com.kakastudio.sms.common.paging.PageResponse;
import com.kakastudio.sms.enrollment.domain.Enrollment;
import com.kakastudio.sms.enrollment.domain.EnrollmentStatus;
import com.kakastudio.sms.enrollment.repository.EnrollmentRepository;
import com.kakastudio.sms.notification.email.domain.OutboxStatus;
import com.kakastudio.sms.notification.email.service.EmailComposer;
import com.kakastudio.sms.notification.email.service.EmailOutboxService;
import com.kakastudio.sms.notification.message.domain.Audience;
import com.kakastudio.sms.notification.message.domain.Notification;
import com.kakastudio.sms.notification.message.domain.NotificationRecipient;
import com.kakastudio.sms.notification.message.dto.AdminNotificationRequest;
import com.kakastudio.sms.notification.message.dto.InboxItem;
import com.kakastudio.sms.notification.message.dto.NotificationRequest;
import com.kakastudio.sms.notification.message.dto.SentNotification;
import com.kakastudio.sms.notification.message.repository.EmailStatusCount;
import com.kakastudio.sms.notification.message.repository.NotificationRecipientRepository;
import com.kakastudio.sms.notification.message.repository.NotificationRepository;
import com.kakastudio.sms.notification.message.service.NotificationService;
import com.kakastudio.sms.professor.domain.Professor;
import com.kakastudio.sms.professor.repository.ProfessorRepository;
import com.kakastudio.sms.section.domain.Section;
import com.kakastudio.sms.section.repository.SectionRepository;
import com.kakastudio.sms.user.domain.Role;
import com.kakastudio.sms.user.domain.User;
import com.kakastudio.sms.user.domain.UserStatus;
import com.kakastudio.sms.user.repository.UserRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationServiceImpl implements NotificationService {

  private final NotificationRepository notifications;
  private final NotificationRecipientRepository recipients;
  private final UserRepository users;
  private final SectionRepository sections;
  private final EnrollmentRepository enrollments;
  private final ProfessorRepository professors;
  private final EmailComposer emails;
  private final EmailOutboxService outbox;
  private final Clock clock;

  public NotificationServiceImpl(
      NotificationRepository notifications,
      NotificationRecipientRepository recipients,
      UserRepository users,
      SectionRepository sections,
      EnrollmentRepository enrollments,
      ProfessorRepository professors,
      EmailComposer emails,
      EmailOutboxService outbox,
      Clock clock) {
    this.notifications = notifications;
    this.recipients = recipients;
    this.users = users;
    this.sections = sections;
    this.enrollments = enrollments;
    this.professors = professors;
    this.emails = emails;
    this.outbox = outbox;
    this.clock = clock;
  }

  @Override
  @Transactional
  public SentNotification sendAsAdmin(Long senderUserId, AdminNotificationRequest request) {
    List<User> audience =
        switch (request.audience()) {
          case ALL_STUDENTS -> users.findByRoleAndStatus(Role.STUDENT, UserStatus.ACTIVE);
          case ALL_PROFESSORS -> users.findByRoleAndStatus(Role.PROFESSOR, UserStatus.ACTIVE);
          case EVERYONE -> {
            List<User> everyone =
                new ArrayList<>(users.findByRoleAndStatus(Role.STUDENT, UserStatus.ACTIVE));
            everyone.addAll(users.findByRoleAndStatus(Role.PROFESSOR, UserStatus.ACTIVE));
            yield everyone;
          }
          case SECTION ->
              throw new ApiException(
                  HttpStatus.BAD_REQUEST,
                  ErrorCode.VALIDATION_FAILED,
                  "Choose all students, all professors or everyone");
        };
    return send(
        sender(senderUserId),
        request.audience(),
        null,
        request.subject(),
        request.body(),
        audience);
  }

  @Override
  @Transactional
  public SentNotification sendToSection(
      Long professorUserId, Long sectionId, NotificationRequest request) {
    Professor professor =
        professors
            .findByUserId(professorUserId)
            .orElseThrow(
                () -> new NotFoundException(ErrorCode.PROFESSOR_NOT_FOUND, "Professor not found"));
    Section section =
        sections
            .findById(sectionId)
            .orElseThrow(
                () -> new NotFoundException(ErrorCode.SECTION_NOT_FOUND, "Section not found"));
    if (!section.getProfessor().getId().equals(professor.getId())) {
      throw new ForbiddenException(ErrorCode.NOT_YOUR_SECTION, "This is not one of your sections");
    }
    List<User> students =
        enrollments.findBySection_IdAndStatus(sectionId, EnrollmentStatus.ENROLLED).stream()
            .map(Enrollment::getStudent)
            .map(student -> student.getUser())
            .toList();
    return send(
        professor.getUser(),
        Audience.SECTION,
        section,
        request.subject(),
        request.body(),
        students);
  }

  private SentNotification send(
      User sender, Audience audience, Section section, String subject, String body, List<User> to) {
    if (to.isEmpty()) {
      throw new RuleViolationException(
          ErrorCode.NO_RECIPIENTS, "Nobody would receive this notification");
    }
    Notification notification =
        notifications.save(
            new Notification(sender, audience, section, subject, body, clock.instant()));
    notification.setRecipientCount(to.size());
    List<NotificationRecipient> rows = new ArrayList<>();
    for (User user : to) {
      NotificationRecipient row = new NotificationRecipient(notification, user);
      row.setEmailOutboxId(
          outbox.enqueue(emails.notification(user, subject, body, sender.fullName())));
      rows.add(row);
    }
    recipients.saveAll(rows);
    return toSent(notification, Map.of(OutboxStatus.PENDING, (long) to.size()));
  }

  @Override
  @Transactional(readOnly = true)
  public List<SentNotification> sentBy(Long senderUserId) {
    List<Notification> sent = notifications.findSentBy(senderUserId);
    Map<Long, Map<OutboxStatus, Long>> statuses = new HashMap<>();
    if (!sent.isEmpty()) {
      for (EmailStatusCount count :
          recipients.countEmailStatuses(sent.stream().map(Notification::getId).toList())) {
        statuses
            .computeIfAbsent(count.notificationId(), id -> new EnumMap<>(OutboxStatus.class))
            .put(count.status(), count.total());
      }
    }
    return sent.stream().map(n -> toSent(n, statuses.getOrDefault(n.getId(), Map.of()))).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<InboxItem> inbox(Long userId, int page, int size) {
    return PageResponse.of(
        recipients.findInbox(
            userId,
            PageRequests.sorted(
                page, size, Sort.by(Sort.Direction.DESC, "notification.createdAt"))),
        NotificationServiceImpl::toInboxItem);
  }

  @Override
  @Transactional(readOnly = true)
  public long unreadCount(Long userId) {
    return recipients.countByUserIdAndReadAtIsNull(userId);
  }

  @Override
  @Transactional
  public void markRead(Long userId, Long recipientId) {
    NotificationRecipient row =
        recipients
            .findById(recipientId)
            .filter(r -> r.getUser().getId().equals(userId))
            .orElseThrow(() ->
                new NotFoundException(ErrorCode.NOTIFICATION_NOT_FOUND, "Notification not found"));
    if (row.getReadAt() == null) {
      row.setReadAt(clock.instant());
    }
  }

  @Override
  @Transactional
  public void markAllRead(Long userId) {
    recipients.markAllRead(userId, clock.instant());
  }

  private User sender(Long userId) {
    return users
        .findById(userId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.REQUEST_FAILED, "Sender not found"));
  }

  private static SentNotification toSent(Notification n, Map<OutboxStatus, Long> statuses) {
    return new SentNotification(
        n.getId(),
        n.getAudience(),
        n.getSection() == null ? null : n.getSection().code(),
        n.getSubject(),
        n.getBody(),
        n.getCreatedAt(),
        n.getRecipientCount(),
        statuses.getOrDefault(OutboxStatus.SENT, 0L),
        statuses.getOrDefault(OutboxStatus.PENDING, 0L),
        statuses.getOrDefault(OutboxStatus.FAILED, 0L));
  }

  private static InboxItem toInboxItem(NotificationRecipient row) {
    Notification n = row.getNotification();
    return new InboxItem(
        row.getId(),
        n.getSubject(),
        n.getBody(),
        n.getSender().fullName(),
        n.getAudience(),
        n.getSection() == null ? null : n.getSection().code(),
        n.getCreatedAt(),
        row.getReadAt() != null);
  }
}
