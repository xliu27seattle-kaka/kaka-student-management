package com.kakastudio.sms.transcript.service.impl;

import com.kakastudio.sms.common.error.ErrorCode;
import com.kakastudio.sms.common.error.NotFoundException;
import com.kakastudio.sms.enrollment.domain.Enrollment;
import com.kakastudio.sms.enrollment.domain.EnrollmentStatus;
import com.kakastudio.sms.enrollment.repository.EnrollmentRepository;
import com.kakastudio.sms.grade.domain.GpaCalculator;
import com.kakastudio.sms.grade.domain.GradeScale;
import com.kakastudio.sms.grade.domain.GradedCredit;
import com.kakastudio.sms.section.domain.Section;
import com.kakastudio.sms.semester.domain.Semester;
import com.kakastudio.sms.student.domain.AcademicStatus;
import com.kakastudio.sms.student.domain.Student;
import com.kakastudio.sms.student.repository.StudentRepository;
import com.kakastudio.sms.transcript.dto.RowStatus;
import com.kakastudio.sms.transcript.dto.TermRecord;
import com.kakastudio.sms.transcript.dto.TranscriptResponse;
import com.kakastudio.sms.transcript.dto.TranscriptRow;
import com.kakastudio.sms.transcript.service.TranscriptService;
import com.kakastudio.sms.user.domain.User;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TranscriptServiceImpl implements TranscriptService {

  private final StudentRepository students;
  private final EnrollmentRepository enrollments;
  private final GradeScale gradeScale;

  public TranscriptServiceImpl(
      StudentRepository students, EnrollmentRepository enrollments, GradeScale gradeScale) {
    this.students = students;
    this.enrollments = enrollments;
    this.gradeScale = gradeScale;
  }

  @Override
  @Transactional(readOnly = true)
  public TranscriptResponse transcript(Long studentId) {
    Student student = find(studentId);
    List<Enrollment> history =
        enrollments.findHistory(studentId).stream()
            .filter(e -> e.getStatus() != EnrollmentStatus.DROPPED)
            .toList();

    Map<Semester, List<Enrollment>> bySemester =
        history.stream()
            .sorted(
                Comparator.comparing((Enrollment e) -> e.getSection().getSemester().getStartDate()))
            .collect(
                Collectors.groupingBy(
                    e -> e.getSection().getSemester(), LinkedHashMap::new, Collectors.toList()));

    List<TermRecord> terms =
        bySemester.entrySet().stream()
            .map(entry -> term(entry.getKey(), entry.getValue()))
            .toList();

    User user = student.getUser();
    return new TranscriptResponse(
        student.getId(),
        student.getStudentNo(),
        user.getFirstName(),
        user.getLastName(),
        user.getEmail(),
        student.getAcademicStatus(),
        student.getCertificateNo(),
        student.getGraduatedAt(),
        student.getAcademicStatus() == AcademicStatus.GRADUATED,
        GpaCalculator.gpa(gradedCredits(history)).orElse(null),
        earnedCredits(history),
        terms);
  }

  @Override
  @Transactional(readOnly = true)
  public int earnedCredits(Long studentId) {
    find(studentId);
    return earnedCredits(enrollments.findHistory(studentId));
  }

  private TermRecord term(Semester semester, List<Enrollment> attempts) {
    List<TranscriptRow> rows =
        attempts.stream()
            .sorted(Comparator.comparing((Enrollment e) -> e.getSection().getCourse().getCode()))
            .map(TranscriptServiceImpl::row)
            .toList();
    List<GradedCredit> graded = gradedCredits(attempts);
    return new TermRecord(
        semester.getId(),
        semester.getCode(),
        semester.getName(),
        semester.getStatus(),
        GpaCalculator.gpa(graded).orElse(null),
        graded.stream().mapToInt(GradedCredit::credits).sum(),
        rows);
  }

  private static TranscriptRow row(Enrollment e) {
    Section section = e.getSection();
    RowStatus status =
        switch (e.getStatus()) {
          case COMPLETED -> RowStatus.GRADED;
          case WITHDRAWN -> RowStatus.WITHDRAWN;
          default -> RowStatus.IN_PROGRESS;
        };
    return new TranscriptRow(
        section.getCourse().getCode(),
        section.getCourse().getTitle(),
        section.code(),
        section.getProfessor().getUser().fullName(),
        section.getCredits(),
        status,
        status == RowStatus.WITHDRAWN ? null : e.getScore(),
        status == RowStatus.WITHDRAWN ? "W" : e.getLetterGrade(),
        status == RowStatus.GRADED ? e.getGradePoints() : null);
  }

  /** Graded attempts in ended semesters: completed rows with points. F counts, W does not. */
  private static List<GradedCredit> gradedCredits(List<Enrollment> attempts) {
    return attempts.stream()
        .filter(e -> e.getStatus() == EnrollmentStatus.COMPLETED && e.getGradePoints() != null)
        .map(e -> new GradedCredit(e.getSection().getCredits(), e.getGradePoints()))
        .toList();
  }

  private int earnedCredits(List<Enrollment> history) {
    Map<Long, Integer> passed = new LinkedHashMap<>();
    for (Enrollment e : history) {
      if (e.getStatus() == EnrollmentStatus.COMPLETED
          && e.getScore() != null
          && gradeScale.isPassing(e.getScore())) {
        passed.putIfAbsent(e.getSection().getCourse().getId(), e.getSection().getCredits());
      }
    }
    return passed.values().stream().mapToInt(Integer::intValue).sum();
  }

  private Student find(Long id) {
    return students
        .findById(id)
        .orElseThrow(() -> new NotFoundException(ErrorCode.STUDENT_NOT_FOUND, "Student not found"));
  }
}
