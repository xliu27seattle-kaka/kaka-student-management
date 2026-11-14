package com.kakastudio.sms.student.dto;

import com.kakastudio.sms.user.domain.UserStatus;

public record RegisterResponse(String studentNo, UserStatus status) {}
