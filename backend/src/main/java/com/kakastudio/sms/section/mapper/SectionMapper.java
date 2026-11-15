package com.kakastudio.sms.section.mapper;

import com.kakastudio.sms.enrollment.domain.Enrollment;
import com.kakastudio.sms.section.domain.MeetingTime;
import com.kakastudio.sms.section.domain.Section;
import com.kakastudio.sms.section.dto.MeetingDto;
import com.kakastudio.sms.section.dto.RosterEntry;
import com.kakastudio.sms.section.dto.SectionDetail;
import com.kakastudio.sms.section.dto.SectionResponse;
import com.kakastudio.sms.user.domain.User;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SectionMapper {

  public SectionResponse toResponse(Section section, long enrolledCount) {
    return new SectionResponse(
        section.getId(),
        section.code(),
        section.getLetter(),
        section.getCourse().getId(),
        section.getCourse().getCode(),
        section.getCourse().getTitle(),
        section.getCredits(),
        section.getSemester().getId(),
        section.getSemester().getCode(),
        section.getSemester().getName(),
        section.getSemester().getStatus(),
        section.getProfessor().getId(),
        section.getProfessor().getUser().fullName(),
        section.getCapacity(),
        enrolledCount,
        section.getRoom(),
        section.getMeetings().stream().map(SectionMapper::toDto).toList());
  }

  public SectionDetail toDetail(Section section, List<Enrollment> roster, long enrolledCount) {
    return new SectionDetail(
        toResponse(section, enrolledCount), roster.stream().map(this::toRosterEntry).toList());
  }

  public RosterEntry toRosterEntry(Enrollment enrollment) {
    User user = enrollment.getStudent().getUser();
    return new RosterEntry(
        enrollment.getId(),
        enrollment.getStudent().getId(),
        enrollment.getStudent().getStudentNo(),
        user.getFirstName(),
        user.getLastName(),
        user.getEmail(),
        enrollment.getStatus(),
        enrollment.getScore(),
        enrollment.getLetterGrade());
  }

  public static MeetingDto toDto(MeetingTime meeting) {
    return new MeetingDto(meeting.getDay(), meeting.getStart(), meeting.getEnd(), meeting.label());
  }
}
