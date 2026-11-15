package com.kakastudio.sms.section.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One weekly class meeting, such as Monday 10:00 to 11:15.
 *
 * <p>The collision rule used everywhere: two meetings on the same day collide unless there are at
 * least {@link #MIN_GAP_MINUTES} minutes between the end of one and the start of the other.
 */
@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingTime {

  /** Time people need to get from one class to the next. */
  public static final int MIN_GAP_MINUTES = 10;

  @Enumerated(EnumType.STRING)
  @Column(name = "day_of_week", nullable = false)
  private DayOfWeek day;

  @Column(name = "start_time", nullable = false)
  private LocalTime start;

  @Column(name = "end_time", nullable = false)
  private LocalTime end;

  public MeetingTime(DayOfWeek day, LocalTime start, LocalTime end) {
    this.day = day;
    this.start = start;
    this.end = end;
  }

  public boolean isValid() {
    return start.isBefore(end);
  }

  /** Same day and less than the minimum gap apart (or overlapping). */
  public boolean collidesWith(MeetingTime other) {
    return day == other.day
        && minutes(start) < minutes(other.end) + MIN_GAP_MINUTES
        && minutes(other.start) < minutes(end) + MIN_GAP_MINUTES;
  }

  /** Same day and actually sharing some minutes. */
  public boolean overlaps(MeetingTime other) {
    return day == other.day
        && minutes(start) < minutes(other.end)
        && minutes(other.start) < minutes(end);
  }

  /** Minutes since midnight, so a late class never wraps around to the next morning. */
  private static int minutes(LocalTime time) {
    return time.getHour() * 60 + time.getMinute();
  }

  public String dayLabel() {
    return day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
  }

  /** For example "Mon 10:00–11:15". */
  public String label() {
    return dayLabel() + " " + start + "–" + end;
  }
}
