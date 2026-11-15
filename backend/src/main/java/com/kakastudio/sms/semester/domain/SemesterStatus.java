package com.kakastudio.sms.semester.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Where a semester is in its life. Only an administrator moves it, one step at a time; dates never
 * change it.
 */
public enum SemesterStatus {
  /** Sections are being built. Students and professors cannot see it. */
  DRAFT,
  /** Enrollment is open. */
  READY,
  /** Classes run: no new enrollment, withdrawals allowed, professors grade. */
  STARTED,
  /** Grades are final. */
  ENDED;

  public Set<SemesterStatus> next() {
    return switch (this) {
      case DRAFT -> EnumSet.of(READY);
      case READY -> EnumSet.of(STARTED, DRAFT);
      case STARTED -> EnumSet.of(ENDED);
      case ENDED -> EnumSet.noneOf(SemesterStatus.class);
    };
  }

  public boolean canMoveTo(SemesterStatus target) {
    return next().contains(target);
  }

  public boolean isVisibleToMembers() {
    return this != DRAFT;
  }
}
