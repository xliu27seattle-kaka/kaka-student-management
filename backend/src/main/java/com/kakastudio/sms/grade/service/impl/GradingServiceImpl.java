package com.kakastudio.sms.grade.service.impl;

import com.kakastudio.sms.common.error.ErrorCode;
import com.kakastudio.sms.common.error.ForbiddenException;
import com.kakastudio.sms.common.error.NotFoundException;
import com.kakastudio.sms.common.error.RuleViolationException;
import com.kakastudio.sms.enrollment.domain.Enrollment;
import com.kakastudio.sms.enrollment.domain.EnrollmentStatus;
import com.kakastudio.sms.enrollment.repository.EnrollmentRepository;
import com.kakastudio.sms.grade.domain.Grade;
import com.kakastudio.sms.grade.domain.GradeScale;
import com.kakastudio.sms.grade.dto.GradeEntry;
import com.kakastudio.sms.grade.service.GradingService;
import com.kakastudio.sms.professor.domain.Professor;
import com.kakastudio.sms.professor.repository.ProfessorRepository;
import com.kakastudio.sms.section.domain.CourseSection;
import com.kakastudio.sms.section.dto.CourseSectionDetail;
import com.kakastudio.sms.section.dto.CourseSectionResponse;
import com.kakastudio.sms.section.mapper.CourseSectionMapper;
import com.kakastudio.sms.section.repository.CourseSectionRepository;
import com.kakastudio.sms.semester.domain.SemesterStatus;
import com.kakastudio.sms.semester.service.SemesterLock;
import java.util.EnumSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GradingServiceImpl implements GradingService {

  private final CourseSectionRepository sections;
  private final EnrollmentRepository enrollments;
  private final ProfessorRepository professors;
  private final GradeScale gradeScale;
  private final CourseSectionMapper mapper;
  private final SemesterLock semesterLock;

  public GradingServiceImpl(
      CourseSectionRepository sections,
      EnrollmentRepository enrollments,
      ProfessorRepository professors,
      GradeScale gradeScale,
      CourseSectionMapper mapper,
      SemesterLock semesterLock) {
    this.sections = sections;
    this.enrollments = enrollments;
    this.professors = professors;
    this.gradeScale = gradeScale;
    this.mapper = mapper;
    this.semesterLock = semesterLock;
  }

  @Override
  @Transactional(readOnly = true)
  public List<CourseSectionResponse> mySections(Long professorUserId, Long semesterId) {
    Professor professor = professor(professorUserId);
    return sections
        .findByProfessorAndSemesterStatuses(
            professor.getId(),
            EnumSet.of(SemesterStatus.READY, SemesterStatus.STARTED, SemesterStatus.ENDED))
        .stream()
        .filter(s -> semesterId == null || s.getSemester().getId().equals(semesterId))
        .map(s -> mapper.toResponse(s, enrolled(s.getId())))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public CourseSectionDetail mySection(Long professorUserId, Long sectionId) {
    CourseSection section = ownSection(professorUserId, sectionId);
    return detail(section);
  }

  @Override
  @Transactional
  public CourseSectionDetail grade(Long professorUserId, Long sectionId, List<GradeEntry> grades) {
    CourseSection section = ownSection(professorUserId, sectionId);
    semesterLock.forUse(section.getSemester()); // the semester cannot end while grades are saved
    if (section.getSemester().getStatus() != SemesterStatus.STARTED) {
      throw new RuleViolationException(
          ErrorCode.GRADING_CLOSED, "Grades can be entered only while the semester is running");
    }
    for (GradeEntry entry : grades) {
      Enrollment enrollment =
          enrollments
              .findById(entry.enrollmentId())
              .filter(e -> e.getSection().getId().equals(sectionId))
              .filter(e -> e.getStatus() == EnrollmentStatus.ENROLLED)
              .orElseThrow(() ->
                  new RuleViolationException(
                      ErrorCode.GRADE_TARGET_INVALID,
                      "Enrollment "
                          + entry.enrollmentId()
                          + " is not an active seat in this section"));
      if (entry.score() == null) {
        enrollment.clearGrade();
      } else {
        Grade grade = gradeScale.gradeFor(entry.score());
        enrollment.setScore(entry.score());
        enrollment.setLetterGrade(grade.letter());
        enrollment.setGradePoints(grade.points());
      }
    }
    return detail(section);
  }

  private CourseSectionDetail detail(CourseSection section) {
    return mapper.toDetail(
        section, enrollments.findRoster(section.getId()), enrolled(section.getId()));
  }

  private CourseSection ownSection(Long professorUserId, Long sectionId) {
    Professor professor = professor(professorUserId);
    CourseSection section =
        sections
            .findById(sectionId)
            .filter(s -> s.getSemester().getStatus().isVisibleToMembers())
            .orElseThrow(
                () -> new NotFoundException(ErrorCode.SECTION_NOT_FOUND, "Section not found"));
    if (!section.getProfessor().getId().equals(professor.getId())) {
      throw new ForbiddenException(ErrorCode.NOT_YOUR_SECTION, "This is not one of your sections");
    }
    return section;
  }

  private Professor professor(Long userId) {
    return professors
        .findByUserId(userId)
        .orElseThrow(
            () -> new NotFoundException(ErrorCode.PROFESSOR_NOT_FOUND, "Professor not found"));
  }

  private long enrolled(Long sectionId) {
    return enrollments.countBySectionIdAndStatus(sectionId, EnrollmentStatus.ENROLLED);
  }
}
