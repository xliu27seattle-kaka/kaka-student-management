package com.kakastudio.sms.enrollment.service.impl;

import com.kakastudio.sms.common.error.ErrorCode;
import com.kakastudio.sms.common.error.NotFoundException;
import com.kakastudio.sms.common.error.RuleViolationException;
import com.kakastudio.sms.enrollment.domain.Enrollment;
import com.kakastudio.sms.enrollment.domain.EnrollmentStatus;
import com.kakastudio.sms.enrollment.dto.EnrollmentResponse;
import com.kakastudio.sms.enrollment.dto.Offering;
import com.kakastudio.sms.enrollment.dto.ReasonDto;
import com.kakastudio.sms.enrollment.repository.EnrollmentRepository;
import com.kakastudio.sms.enrollment.repository.SectionSeatCount;
import com.kakastudio.sms.enrollment.rules.EnrollmentContext;
import com.kakastudio.sms.enrollment.rules.EnrollmentRule;
import com.kakastudio.sms.enrollment.rules.Ineligibility;
import com.kakastudio.sms.enrollment.service.EnrollmentService;
import com.kakastudio.sms.section.domain.Section;
import com.kakastudio.sms.section.mapper.SectionMapper;
import com.kakastudio.sms.section.repository.SectionRepository;
import com.kakastudio.sms.semester.domain.Semester;
import com.kakastudio.sms.semester.domain.SemesterStatus;
import com.kakastudio.sms.semester.repository.SemesterRepository;
import com.kakastudio.sms.semester.service.SemesterLock;
import com.kakastudio.sms.student.domain.Student;
import com.kakastudio.sms.student.repository.StudentRepository;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnrollmentServiceImpl implements EnrollmentService {

  private final StudentRepository students;
  private final SectionRepository sections;
  private final SemesterRepository semesters;
  private final EnrollmentRepository enrollments;
  private final List<EnrollmentRule> rules;
  private final SectionMapper sectionMapper;
  private final SemesterLock semesterLock;
  private final Clock clock;

  public EnrollmentServiceImpl(
      StudentRepository students,
      SectionRepository sections,
      SemesterRepository semesters,
      EnrollmentRepository enrollments,
      List<EnrollmentRule> rules,
      SectionMapper sectionMapper,
      SemesterLock semesterLock,
      Clock clock) {
    this.students = students;
    this.sections = sections;
    this.semesters = semesters;
    this.enrollments = enrollments;
    this.rules = rules;
    this.sectionMapper = sectionMapper;
    this.semesterLock = semesterLock;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public List<Offering> offerings(Long studentId, Long semesterId) {
    Student student = findStudent(studentId);
    Semester semester =
        semesters
            .findById(semesterId)
            .filter(s -> s.getStatus().isVisibleToMembers())
            .orElseThrow(
                () -> new NotFoundException(ErrorCode.SEMESTER_NOT_FOUND, "Semester not found"));
    Map<Long, Long> taken =
        enrollments.countSeatsTaken(semester.getId(), EnrollmentStatus.ENROLLED).stream()
            .collect(Collectors.toMap(SectionSeatCount::sectionId, SectionSeatCount::taken));
    List<Enrollment> history = enrollments.findHistory(studentId);
    Map<Long, Enrollment> mine =
        history.stream()
            .filter(e -> e.getStatus() != EnrollmentStatus.DROPPED)
            .collect(
                Collectors.toMap(e -> e.getSection().getId(), Function.identity(), (a, b) -> a));
    return sections.findBySemesterId(semester.getId()).stream()
        .map(section -> {
          long seats = taken.getOrDefault(section.getId(), 0L);
          List<ReasonDto> reasons =
              evaluate(new EnrollmentContext(student, section, seats, history)).stream()
                  .map(i -> new ReasonDto(i.reason(), i.message()))
                  .toList();
          Enrollment own = mine.get(section.getId());
          return new Offering(
              sectionMapper.toResponse(section, seats),
              reasons.isEmpty(),
              reasons,
              own == null ? null : own.getId(),
              own == null ? null : own.getStatus());
        })
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<EnrollmentResponse> enrollments(Long studentId, Long semesterId) {
    findStudent(studentId);
    return enrollments.findHistory(studentId).stream()
        .filter(e -> e.getStatus() != EnrollmentStatus.DROPPED)
        .filter(e -> semesterId == null || e.getSection().getSemester().getId().equals(semesterId))
        .sorted(
            Comparator.comparing((Enrollment e) -> e.getSection().getSemester().getStartDate())
                .reversed()
                .thenComparing(e -> e.getSection().code()))
        .map(this::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public EnrollmentResponse enroll(Long studentId, Long sectionId) {
    // Fixed order: the student, then the section, then the semester (shared), so two requests
    // never deadlock and the semester cannot change status in the middle.
    Student student =
        students
            .findWithLockById(studentId)
            .orElseThrow(
                () -> new NotFoundException(ErrorCode.STUDENT_NOT_FOUND, "Student not found"));
    Section section =
        sections
            .findWithLockById(sectionId)
            .orElseThrow(
                () -> new NotFoundException(ErrorCode.SECTION_NOT_FOUND, "Section not found"));
    semesterLock.forUse(section.getSemester());
    long seats = enrollments.countBySectionIdAndStatus(sectionId, EnrollmentStatus.ENROLLED);
    List<Ineligibility> problems =
        evaluate(
            new EnrollmentContext(student, section, seats, enrollments.findHistory(studentId)));
    if (!problems.isEmpty()) {
      throw new RuleViolationException(ErrorCode.NOT_ELIGIBLE, problems.getFirst().message());
    }
    Optional<Enrollment> previous = enrollments.findByStudentIdAndSectionId(studentId, sectionId);
    Enrollment enrollment;
    if (previous.isPresent()) {
      // Dropped earlier while enrollment was open: the same row comes back.
      enrollment = previous.get();
      enrollment.setStatus(EnrollmentStatus.ENROLLED);
      enrollment.clearGrade();
      enrollment.setEnrolledAt(clock.instant());
    } else {
      enrollment = enrollments.save(new Enrollment(student, section));
    }
    return toResponse(enrollment);
  }

  @Override
  @Transactional
  public EnrollmentResponse drop(Long studentId, Long enrollmentId) {
    Enrollment enrollment =
        enrollments
            .findById(enrollmentId)
            .filter(e -> e.getStudent().getId().equals(studentId))
            .orElseThrow(() ->
                new NotFoundException(ErrorCode.ENROLLMENT_NOT_FOUND, "Enrollment not found"));
    semesterLock.forUse(enrollment.getSection().getSemester());
    if (!enrollment.isEnrolled()) {
      throw new RuleViolationException(
          ErrorCode.ENROLLMENT_LOCKED, "This seat is no longer active");
    }
    SemesterStatus status = enrollment.getSection().getSemester().getStatus();
    switch (status) {
      case READY -> enrollment.setStatus(EnrollmentStatus.DROPPED);
      case STARTED -> enrollment.setStatus(EnrollmentStatus.WITHDRAWN);
      default ->
          throw new RuleViolationException(
              ErrorCode.ENROLLMENT_LOCKED,
              "Enrollments cannot change in a " + status + " semester");
    }
    enrollment.clearGrade();
    return toResponse(enrollment);
  }

  /** Every rule that fails, in order. */
  private List<Ineligibility> evaluate(EnrollmentContext context) {
    return rules.stream().map(rule -> rule.check(context)).flatMap(Optional::stream).toList();
  }

  private EnrollmentResponse toResponse(Enrollment enrollment) {
    Section section = enrollment.getSection();
    long seats = enrollments.countBySectionIdAndStatus(section.getId(), EnrollmentStatus.ENROLLED);
    return new EnrollmentResponse(
        enrollment.getId(),
        enrollment.getStatus(),
        sectionMapper.toResponse(section, seats),
        enrollment.getScore(),
        enrollment.getLetterGrade());
  }

  private Student findStudent(Long id) {
    return students
        .findById(id)
        .orElseThrow(() -> new NotFoundException(ErrorCode.STUDENT_NOT_FOUND, "Student not found"));
  }
}
