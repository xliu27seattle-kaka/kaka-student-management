package com.kakastudio.sms.auth.service;

import com.kakastudio.sms.auth.dto.ChangePasswordRequest;
import com.kakastudio.sms.auth.dto.LoginRequest;
import com.kakastudio.sms.auth.dto.LoginResponse;
import com.kakastudio.sms.auth.dto.UserSummary;

public interface AuthService {

  /** Checks email and password and, for an active account, returns an access token. */
  LoginResponse login(LoginRequest request);

  /** The signed-in user. */
  UserSummary me();

  /** Changes the signed-in user's own password; the current one must be given. */
  void changePassword(ChangePasswordRequest request);
}
