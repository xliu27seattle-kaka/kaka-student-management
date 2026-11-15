package com.kakastudio.sms.section.domain;

import static com.kakastudio.sms.testsupport.Catalog.meeting;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MeetingTimeTest {

  private final MeetingTime morning = meeting(MONDAY, "10:00", "11:15");

  @Test
  void tenMinutesBetweenMeetingsIsEnough() {
    assertThat(morning.collidesWith(meeting(MONDAY, "11:25", "12:40"))).isFalse();
    assertThat(meeting(MONDAY, "08:00", "09:50").collidesWith(morning)).isFalse();
  }

  @Test
  void lessThanTenMinutesCollides() {
    assertThat(morning.collidesWith(meeting(MONDAY, "11:20", "12:40"))).isTrue();
    assertThat(meeting(MONDAY, "08:00", "09:51").collidesWith(morning)).isTrue();
  }

  @Test
  void overlappingMeetingsCollideAndOverlap() {
    MeetingTime other = meeting(MONDAY, "11:00", "12:00");
    assertThat(morning.collidesWith(other)).isTrue();
    assertThat(morning.overlaps(other)).isTrue();
    assertThat(morning.overlaps(meeting(MONDAY, "11:20", "12:00"))).isFalse();
  }

  @Test
  void differentDaysNeverCollide() {
    assertThat(morning.collidesWith(meeting(WEDNESDAY, "10:00", "11:15"))).isFalse();
  }

  @Test
  void lateEveningMeetingsDoNotWrapPastMidnight() {
    assertThat(meeting(MONDAY, "22:00", "23:55").collidesWith(meeting(MONDAY, "00:00", "01:00")))
        .isFalse();
  }

  @Test
  void endMustBeAfterStart() {
    assertThat(meeting(MONDAY, "10:00", "11:00").isValid()).isTrue();
    assertThat(meeting(MONDAY, "11:00", "11:00").isValid()).isFalse();
  }

  @Test
  void hasAReadableLabel() {
    assertThat(morning.label()).isEqualTo("Mon 10:00–11:15");
  }
}
