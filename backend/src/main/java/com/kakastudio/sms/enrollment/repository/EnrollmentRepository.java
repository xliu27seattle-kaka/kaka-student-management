package com.kakastudio.sms.enrollment.repository;

import com.kakastudio.sms.enrollment.domain.Enrollment;
import com.kakastudio.sms.enrollment.domain.EnrollmentStatus;
import com.kakastudio.sms.semester.domain.SemesterStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

  long countBySectionIdAndStatus(Long sectionId, EnrollmentStatus status);

  long countBySection_Semester_IdAndStatus(Long semesterId, EnrollmentStatus status);

  /** History that matters: anything but a section dropped while enrollment was open. */
  boolean existsByStudentIdAndStatusNot(Long studentId, EnrollmentStatus status);

  void deleteByStudentId(Long studentId);

  Optional<Enrollment> findByStudentIdAndSectionId(Long studentId, Long sectionId);

  List<Enrollment> findBySection_IdAndStatus(Long sectionId, EnrollmentStatus status);

  boolean existsByStudent_IdAndStatusAndSection_Semester_Status(
      Long studentId, EnrollmentStatus status, SemesterStatus semesterStatus);

  List<Enrollment> findByStudent_IdAndStatusAndSection_Semester_Status(
      Long studentId, EnrollmentStatus status, SemesterStatus semesterStatus);

  List<Enrollment> findByStudent_IdAndStatusAndSection_Semester_StatusIn(
      Long studentId, EnrollmentStatus status, Collection<SemesterStatus> semesterStatuses);

  /** Seats taken per section of one semester, in one query. */
  @Query(
      "select new com.kakastudio.sms.enrollment.repository.SectionSeatCount(e.section.id, count(e))"
          + " from Enrollment e where e.section.semester.id = :semesterId and e.status = :status"
          + " group by e.section.id")
  List<SectionSeatCount> countSeatsTaken(
      @Param("semesterId") Long semesterId, @Param("status") EnrollmentStatus status);

  /** Every enrollment of a student, with section, course, semester and professor loaded. */
  @Query(
      "select e from Enrollment e join fetch e.section s join fetch s.course join fetch s.semester"
          + " join fetch s.professor p join fetch p.user where e.student.id = :studentId")
  List<Enrollment> findHistory(@Param("studentId") Long studentId);

  /** The class list of a section: everyone except students who dropped. */
  @Query(
      "select e from Enrollment e join fetch e.student st join fetch st.user u"
          + " where e.section.id = :sectionId"
          + " and e.status <> com.kakastudio.sms.enrollment.domain.EnrollmentStatus.DROPPED"
          + " order by u.lastName, u.firstName")
  List<Enrollment> findRoster(@Param("sectionId") Long sectionId);

  /** Codes of the sections of a semester that still have an enrolled student without a score. */
  @Query(
      "select concat(c.code, '-', s.letter) from Enrollment e join e.section s join s.course c"
          + " where s.semester.id = :semesterId"
          + " and e.status = com.kakastudio.sms.enrollment.domain.EnrollmentStatus.ENROLLED"
          + " and e.score is null group by c.code, s.letter order by c.code, s.letter")
  List<String> findUngradedSectionCodes(@Param("semesterId") Long semesterId);

  /** Moves every enrollment of a semester from one status to another, in one statement. */
  @Modifying(flushAutomatically = true)
  @Query(
      "update Enrollment e set e.status = :to, e.updatedAt = :now where e.status = :from"
          + " and e.section.id in (select s.id from Section s where s.semester.id = :semesterId)")
  int completeEnrolled(
      @Param("semesterId") Long semesterId,
      @Param("from") EnrollmentStatus from,
      @Param("to") EnrollmentStatus to,
      @Param("now") Instant now);
}
