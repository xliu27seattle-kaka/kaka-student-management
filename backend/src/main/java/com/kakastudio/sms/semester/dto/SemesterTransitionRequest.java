package com.kakastudio.sms.semester.dto;

import com.kakastudio.sms.semester.domain.SemesterStatus;
import jakarta.validation.constraints.NotNull;

public record SemesterTransitionRequest(@NotNull SemesterStatus target) {}
