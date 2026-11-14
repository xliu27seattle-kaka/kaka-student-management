package com.kakastudio.sms.professor.service;

import com.kakastudio.sms.common.paging.PageResponse;
import com.kakastudio.sms.professor.dto.ProfessorCreateRequest;
import com.kakastudio.sms.professor.dto.ProfessorResponse;
import com.kakastudio.sms.professor.dto.ProfessorUpdateRequest;

/** What an administrator does with professors. */
public interface ProfessorAdminService {

  PageResponse<ProfessorResponse> list(String query, int page, int size);

  /** Creates an active professor and emails them a temporary password. */
  ProfessorResponse create(ProfessorCreateRequest request);

  ProfessorResponse update(Long id, ProfessorUpdateRequest request);

  /** {@code ACTIVE} to {@code INACTIVE}. */
  ProfessorResponse deactivate(Long id);

  /** {@code INACTIVE} to {@code ACTIVE}. */
  ProfessorResponse reactivate(Long id);

  void delete(Long id);
}
