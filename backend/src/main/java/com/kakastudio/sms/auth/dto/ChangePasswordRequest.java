package com.kakastudio.sms.auth.dto;

import com.kakastudio.sms.user.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
    @NotBlank String currentPassword, @NotBlank @ValidPassword String newPassword) {}
