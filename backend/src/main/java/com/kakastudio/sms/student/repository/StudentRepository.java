package com.kakastudio.sms.student.repository;

import com.kakastudio.sms.student.domain.Student;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StudentRepository
    extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {

  Optional<Student> findByUserId(Long userId);

  /** Loads each student's account in the same query, so listing does not run one query per row. */
  @Override
  @EntityGraph(attributePaths = "user")
  Page<Student> findAll(Specification<Student> specification, Pageable pageable);
}
