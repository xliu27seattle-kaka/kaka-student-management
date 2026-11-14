package com.kakastudio.sms.professor.repository;

import com.kakastudio.sms.professor.domain.Professor;
import com.kakastudio.sms.user.domain.UserStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProfessorRepository
    extends JpaRepository<Professor, Long>, JpaSpecificationExecutor<Professor> {

  Optional<Professor> findByUserId(Long userId);

  @Override
  @EntityGraph(attributePaths = "user")
  Page<Professor> findAll(Specification<Professor> specification, Pageable pageable);

  /** The directory: every professor whose account has the given status, by name. */
  @EntityGraph(attributePaths = "user")
  List<Professor> findAllByUserStatusOrderByUserLastNameAscUserFirstNameAsc(UserStatus status);
}
