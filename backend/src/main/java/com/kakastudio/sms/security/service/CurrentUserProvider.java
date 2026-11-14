package com.kakastudio.sms.security.service;

import com.kakastudio.sms.security.dto.CurrentUser;

public interface CurrentUserProvider {

  /**
   * @throws com.kakastudio.sms.common.error.UnauthorizedException if nobody is signed in
   */
  CurrentUser require();
}
