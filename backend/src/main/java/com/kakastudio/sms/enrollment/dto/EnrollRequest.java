package com.kakastudio.sms.enrollment.dto;

import jakarta.validation.constraints.NotNull;

public record EnrollRequest(@NotNull Long sectionId) {}
