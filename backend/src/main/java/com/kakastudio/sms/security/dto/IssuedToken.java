package com.kakastudio.sms.security.dto;

import java.time.Instant;

public record IssuedToken(String value, Instant expiresAt) {}
