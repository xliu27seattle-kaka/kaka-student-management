package com.kakastudio.sms.student.repository;

import com.kakastudio.sms.student.domain.Student;
import com.kakastudio.sms.user.domain.UserStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentRepository
    extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {

  Optional<Student> findByUserId(Long userId);

  long countByUserStatus(UserStatus status);

  /** Locks the student row: one student's simultaneous requests are handled one after another. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from Student s join fetch s.user where s.id = :id")
  Optional<Student> findWithLockById(@Param("id") Long id);

  /** Loads each student's account in the same query, so listing does not run one query per row. */
  @Override
  @EntityGraph(attributePaths = "user")
  Page<Student> findAll(Specification<Student> specification, Pageable pageable);
}
