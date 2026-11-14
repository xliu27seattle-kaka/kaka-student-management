package com.kakastudio.sms.security.service;

import com.kakastudio.sms.security.dto.IssuedToken;
import com.kakastudio.sms.user.domain.User;

public interface TokenService {

  /** Creates a signed access token for a user who has just proved who they are. */
  IssuedToken issue(User user);
}
