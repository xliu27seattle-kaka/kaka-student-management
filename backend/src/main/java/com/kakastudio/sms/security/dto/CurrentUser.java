package com.kakastudio.sms.security.dto;

import com.kakastudio.sms.user.domain.Role;

/** Who is making the current request. */
public record CurrentUser(Long id, String email, Role role) {}
