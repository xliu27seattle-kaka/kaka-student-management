package com.kakastudio.sms.professor.mapper;

import com.kakastudio.sms.professor.domain.AwardEntry;
import com.kakastudio.sms.professor.domain.ExperienceEntry;
import com.kakastudio.sms.professor.domain.Professor;
import com.kakastudio.sms.professor.dto.AwardDto;
import com.kakastudio.sms.professor.dto.ExperienceDto;
import com.kakastudio.sms.professor.dto.ProfessorProfileRequest;
import com.kakastudio.sms.professor.dto.ProfessorProfileResponse;
import com.kakastudio.sms.professor.dto.ProfessorResponse;
import com.kakastudio.sms.user.domain.User;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProfessorMapper {

  private static final String PHOTO_BASE_URL = "/assets/professors/";

  public ProfessorResponse toResponse(Professor professor) {
    User user = professor.getUser();
    return new ProfessorResponse(
        professor.getId(),
        professor.getProfessorNo(),
        user.getEmail(),
        user.getFirstName(),
        user.getLastName(),
        user.getStatus(),
        professor.getTitle());
  }

  public ProfessorProfileResponse toProfile(Professor professor) {
    User user = professor.getUser();
    return new ProfessorProfileResponse(
        professor.getId(),
        professor.getProfessorNo(),
        user.getFirstName(),
        user.getLastName(),
        professor.getTitle(),
        professor.getBio(),
        professor.getPhotoPath() == null ? null : PHOTO_BASE_URL + professor.getPhotoPath(),
        List.copyOf(professor.getExpertise()),
        professor.getExperience().stream()
            .map(e -> new ExperienceDto(e.getRoleTitle(), e.getOrganization(), e.getPeriod()))
            .toList(),
        professor.getAwards().stream().map(a -> new AwardDto(a.getTitle(), a.getYear())).toList());
  }

  /** Copies a submitted profile onto the professor, replacing what was there. */
  public void applyProfile(ProfessorProfileRequest profile, Professor professor) {
    professor.setTitle(profile.title());
    professor.setBio(profile.bio());
    professor.replaceLists(
        profile.expertise().stream().map(String::trim).toList(),
        profile.experience().stream()
            .map(e -> new ExperienceEntry(e.roleTitle(), e.organization(), e.period()))
            .toList(),
        profile.awards().stream().map(a -> new AwardEntry(a.title(), a.year())).toList());
  }
}
