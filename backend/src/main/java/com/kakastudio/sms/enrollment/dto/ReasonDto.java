package com.kakastudio.sms.enrollment.dto;

import com.kakastudio.sms.enrollment.rules.IneligibilityReason;

public record ReasonDto(IneligibilityReason code, String message) {}
