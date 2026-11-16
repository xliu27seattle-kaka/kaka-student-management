package com.kakastudio.sms.semester.service.impl;

import com.kakastudio.sms.common.error.ConflictException;
import com.kakastudio.sms.common.error.ErrorCode;
import com.kakastudio.sms.common.error.NotFoundException;
import com.kakastudio.sms.common.error.RuleViolationException;
import com.kakastudio.sms.enrollment.domain.EnrollmentStatus;
import com.kakastudio.sms.enrollment.repository.EnrollmentRepository;
import com.kakastudio.sms.section.repository.CourseSectionRepository;
import com.kakastudio.sms.semester.domain.Semester;
import com.kakastudio.sms.semester.domain.SemesterStatus;
import com.kakastudio.sms.semester.dto.SemesterRequest;
import com.kakastudio.sms.semester.dto.SemesterResponse;
import com.kakastudio.sms.semester.repository.SemesterRepository;
import com.kakastudio.sms.semester.service.SemesterLock;
import com.kakastudio.sms.semester.service.SemesterService;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SemesterServiceImpl implements SemesterService {

  private final SemesterRepository semesters;
  private final CourseSectionRepository sections;
  private final EnrollmentRepository enrollments;
  private final SemesterLock semesterLock;
  private final Clock clock;

  public SemesterServiceImpl(
      SemesterRepository semesters,
      CourseSectionRepository sections,
      EnrollmentRepository enrollments,
      SemesterLock semesterLock,
      Clock clock) {
    this.semesters = semesters;
    this.sections = sections;
    this.enrollments = enrollments;
    this.semesterLock = semesterLock;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public List<SemesterResponse> listAll() {
    return semesters.findAllByOrderByStartDateDesc().stream().map(this::toResponse).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<SemesterResponse> listVisible() {
    return semesters.findByStatusNotOrderByStartDateDesc(SemesterStatus.DRAFT).stream()
        .map(this::toResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public SemesterResponse get(Long id) {
    return toResponse(find(id));
  }

  @Override
  @Transactional
  public SemesterResponse create(SemesterRequest request) {
    checkDates(request);
    if (semesters.existsByCode(request.code())) {
      throw codeTaken(request.code());
    }
    return toResponse(
        semesters.save(
            new Semester(request.code(), request.name(), request.startDate(), request.endDate())));
  }

  @Override
  @Transactional
  public SemesterResponse update(Long id, SemesterRequest request) {
    Semester semester = find(id);
    if (semester.getStatus() == SemesterStatus.ENDED) {
      throw new RuleViolationException(
          ErrorCode.SEMESTER_NOT_EDITABLE, "An ended semester cannot be changed");
    }
    checkDates(request);
    if (!semester.getCode().equals(request.code())) {
      if (semester.getStatus() != SemesterStatus.DRAFT) {
        throw new RuleViolationException(
            ErrorCode.SEMESTER_NOT_EDITABLE,
            "The code can only change while the semester is a draft");
      }
      if (semesters.existsByCode(request.code())) {
        throw codeTaken(request.code());
      }
      semester.setCode(request.code());
    }
    semester.setName(request.name());
    semester.setStartDate(request.startDate());
    semester.setEndDate(request.endDate());
    return toResponse(semester);
  }

  @Override
  @Transactional
  public void delete(Long id) {
    Semester semester = find(id);
    if (semester.getStatus() != SemesterStatus.DRAFT || sections.countBySemesterId(id) > 0) {
      throw new ConflictException(
          ErrorCode.SEMESTER_IN_USE, "Only a draft semester without sections can be deleted");
    }
    semesters.delete(semester);
  }

  @Override
  @Transactional(readOnly = true)
  public List<String> ungradedSections(Long id) {
    find(id);
    return enrollments.findUngradedSectionCodes(id);
  }

  @Override
  @Transactional
  public SemesterResponse transition(Long id, SemesterStatus target) {
    Semester semester = find(id);
    semesterLock.forChange(semester); // waits for running enrollments and grade saves
    SemesterStatus current = semester.getStatus();
    if (!current.canMoveTo(target)) {
      throw new RuleViolationException(
          ErrorCode.SEMESTER_TRANSITION_NOT_ALLOWED,
          "A " + current + " semester cannot move to " + target);
    }
    if (target == SemesterStatus.DRAFT) {
      long enrolled =
          enrollments.countBySection_Semester_IdAndStatus(id, EnrollmentStatus.ENROLLED);
      if (enrolled > 0) {
        throw new RuleViolationException(
            ErrorCode.SEMESTER_HAS_ENROLLMENTS,
            enrolled
                + " students are enrolled. They must drop before the semester goes back to draft.");
      }
    }
    if (target == SemesterStatus.ENDED) {
      List<String> ungraded = enrollments.findUngradedSectionCodes(id);
      if (!ungraded.isEmpty()) {
        throw new RuleViolationException(
            ErrorCode.UNGRADED_ENROLLMENTS,
            "Grades are missing in " + String.join(", ", ungraded) + ".");
      }
      enrollments.completeEnrolled(
          id, EnrollmentStatus.ENROLLED, EnrollmentStatus.COMPLETED, clock.instant());
    }
    semester.setStatus(target);
    return toResponse(semester);
  }

  private static void checkDates(SemesterRequest request) {
    if (!request.endDate().isAfter(request.startDate())) {
      throw new RuleViolationException(
          ErrorCode.INVALID_DATE_RANGE, "The end date must be after the start date");
    }
  }

  private SemesterResponse toResponse(Semester semester) {
    Long id = semester.getId();
    return new SemesterResponse(
        id,
        semester.getCode(),
        semester.getName(),
        semester.getStartDate(),
        semester.getEndDate(),
        semester.getStatus(),
        semester.getStatus().next(),
        id == null ? 0 : sections.countBySemesterId(id),
        id == null
            ? 0
            : enrollments.countBySection_Semester_IdAndStatus(id, EnrollmentStatus.ENROLLED));
  }

  private Semester find(Long id) {
    return semesters
        .findById(id)
        .orElseThrow(
            () -> new NotFoundException(ErrorCode.SEMESTER_NOT_FOUND, "Semester not found"));
  }

  private static ConflictException codeTaken(String code) {
    return new ConflictException(
        ErrorCode.SEMESTER_CODE_TAKEN, "Semester " + code + " already exists");
  }
}
