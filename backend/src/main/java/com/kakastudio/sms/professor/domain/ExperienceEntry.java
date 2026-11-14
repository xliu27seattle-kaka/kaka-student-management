package com.kakastudio.sms.professor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** One line of a professor's career: what they did, where, and when. */
@Embeddable
@Getter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExperienceEntry {

  @Column(name = "role_title", nullable = false)
  private String roleTitle;

  @Column(nullable = false)
  private String organization;

  @Column(nullable = false)
  private String period;
}
