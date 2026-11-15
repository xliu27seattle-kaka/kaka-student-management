package com.kakastudio.sms.section.dto;

import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;

/** One weekly meeting, for example {"day":"MONDAY","start":"10:00","end":"11:15"}. */
public record MeetingRequest(
    @NotNull DayOfWeek day, @NotNull LocalTime start, @NotNull LocalTime end) {}
