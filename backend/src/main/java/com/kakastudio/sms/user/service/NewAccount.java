package com.kakastudio.sms.user.service;

import com.kakastudio.sms.user.domain.Role;
import com.kakastudio.sms.user.domain.UserStatus;

/** Everything needed to create an account. {@code email} must already be normalized. */
public record NewAccount(
    String email,
    String firstName,
    String lastName,
    String rawPassword,
    Role role,
    UserStatus status) {}
