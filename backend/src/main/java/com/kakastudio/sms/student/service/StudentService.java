package com.kakastudio.sms.student.service;

import com.kakastudio.sms.common.paging.PageResponse;
import com.kakastudio.sms.student.dto.RegisterRequest;
import com.kakastudio.sms.student.dto.RegisterResponse;
import com.kakastudio.sms.student.dto.StudentCreateRequest;
import com.kakastudio.sms.student.dto.StudentResponse;
import com.kakastudio.sms.student.dto.StudentUpdateRequest;
import com.kakastudio.sms.user.domain.UserStatus;

public interface StudentService {

  /** Self-registration. The account is {@code PENDING} until an administrator decides. */
  RegisterResponse register(RegisterRequest request);

  /**
   * @param status optional filter on the account status
   * @param query optional text matched against student number, email and names
   */
  PageResponse<StudentResponse> list(UserStatus status, String query, int page, int size);

  StudentResponse get(Long id);

  /** Creates an active student and emails them a temporary password. */
  StudentResponse create(StudentCreateRequest request);

  StudentResponse update(Long id, StudentUpdateRequest request);

  /** {@code PENDING} to {@code ACTIVE}. */
  StudentResponse approve(Long id);

  /** {@code PENDING} to {@code REJECTED}. */
  StudentResponse reject(Long id);

  /** {@code ACTIVE} to {@code INACTIVE}. */
  StudentResponse deactivate(Long id);

  /** {@code INACTIVE} to {@code ACTIVE}. */
  StudentResponse reactivate(Long id);

  void delete(Long id);
}
