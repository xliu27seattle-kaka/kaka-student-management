package com.kakastudio.sms.graduation.repository;

import com.kakastudio.sms.graduation.domain.ApplicationStatus;
import com.kakastudio.sms.graduation.domain.GraduationApplication;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GraduationApplicationRepository
    extends JpaRepository<GraduationApplication, Long> {

  /** Locks the application, so two administrators cannot decide it at the same time. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select a from GraduationApplication a where a.id = :id")
  Optional<GraduationApplication> findWithLockById(@Param("id") Long id);

  boolean existsByStudentIdAndStatus(Long studentId, ApplicationStatus status);

  long countByStatus(ApplicationStatus status);

  Optional<GraduationApplication> findFirstByStudentIdOrderByAppliedAtDesc(Long studentId);

  /** Applications with the given status (all when {@code null}), oldest first. */
  @Query(
      "select a from GraduationApplication a join fetch a.student s join fetch s.user"
          + " where :status is null or a.status = :status order by a.appliedAt")
  List<GraduationApplication> findForReview(@Param("status") ApplicationStatus status);
}
