package com.kakastudio.sms.notification.message.domain;

import com.kakastudio.sms.section.domain.Section;
import com.kakastudio.sms.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A message written once and delivered to many recipients, in the app and by email. */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** {@code null} for a note the system wrote. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sender_id")
  private User sender;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Audience audience;

  /** Set when the audience is one section. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "section_id")
  private Section section;

  @Column(nullable = false)
  private String subject;

  @Column(nullable = false, columnDefinition = "text")
  private String body;

  @Column(name = "recipient_count", nullable = false)
  private int recipientCount;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public Notification(
      User sender,
      Audience audience,
      Section section,
      String subject,
      String body,
      Instant createdAt) {
    this.sender = sender;
    this.audience = audience;
    this.section = section;
    this.subject = subject;
    this.body = body;
    this.createdAt = createdAt;
  }
}
