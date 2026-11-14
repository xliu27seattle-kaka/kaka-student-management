package com.kakastudio.sms.auth.dto;

import java.time.Instant;

public record LoginResponse(String accessToken, Instant expiresAt, UserSummary user) {}
