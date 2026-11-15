package com.kakastudio.sms.section.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record MeetingDto(DayOfWeek day, LocalTime start, LocalTime end, String label) {}
