package com.kakastudio.sms.professor.service;

import com.kakastudio.sms.professor.dto.ProfessorProfileRequest;
import com.kakastudio.sms.professor.dto.ProfessorProfileResponse;

/** A professor reading and editing their own self-introduction. */
public interface ProfessorProfileService {

  ProfessorProfileResponse myProfile();

  ProfessorProfileResponse updateMyProfile(ProfessorProfileRequest request);
}
