package com.kakastudio.sms.section.repository;

import com.kakastudio.sms.section.domain.Section;
import com.kakastudio.sms.semester.domain.SemesterStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SectionRepository extends JpaRepository<Section, Long> {

  long countByCourseId(Long courseId);

  long countBySemesterId(Long semesterId);

  boolean existsByProfessorId(Long professorId);

  /** The sections of a semester by course code and letter, with course and professor loaded. */
  @Query(
      "select s from Section s join fetch s.course c join fetch s.semester join fetch s.professor p"
          + " join fetch p.user where s.semester.id = :semesterId order by c.code, s.letter")
  List<Section> findBySemesterId(@Param("semesterId") Long semesterId);

  List<Section> findByProfessorIdAndSemesterId(Long professorId, Long semesterId);

  /** Every section of a course, newest semester first. */
  @Query(
      "select s from Section s join fetch s.semester sem join fetch s.professor p join fetch p.user"
          + " where s.course.id = :courseId order by sem.startDate desc, s.letter")
  List<Section> findByCourse(@Param("courseId") Long courseId);

  @Query(
      "select s.letter from Section s where s.course.id = :courseId and s.semester.id = :semesterId")
  List<String> findLetters(@Param("courseId") Long courseId, @Param("semesterId") Long semesterId);

  /** Locks the section row, so two students cannot take its last seat at the same time. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from Section s where s.id = :id")
  Optional<Section> findWithLockById(@Param("id") Long id);

  /** A professor's sections in semesters with the given statuses, newest semester first. */
  @Query(
      "select s from Section s join fetch s.course c join fetch s.semester sem"
          + " where s.professor.id = :professorId and sem.status in :statuses"
          + " order by sem.startDate desc, c.code, s.letter")
  List<Section> findByProfessorAndSemesterStatuses(
      @Param("professorId") Long professorId,
      @Param("statuses") Collection<SemesterStatus> statuses);
}
