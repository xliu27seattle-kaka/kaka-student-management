package com.kakastudio.sms.professor.service;

import com.kakastudio.sms.professor.dto.ProfessorProfileResponse;
import java.util.List;

/** The professor profiles every signed-in user may read. */
public interface ProfessorDirectoryService {

  /** Active professors, by last name. */
  List<ProfessorProfileResponse> directory();

  /** One active professor's profile page. */
  ProfessorProfileResponse profile(Long id);
}
