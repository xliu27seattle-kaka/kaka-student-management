package com.kakastudio.sms.course.repository;

import com.kakastudio.sms.course.domain.Course;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository
    extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {

  boolean existsByCode(String code);

  long countByActiveTrue();

  /** The whole catalogue with prerequisites, to check a change for cycles. */
  @Query("select distinct c from Course c left join fetch c.prerequisites")
  List<Course> findGraph();

  /** The courses that list the given course as a prerequisite. */
  @Query("select c from Course c join c.prerequisites p where p.id = :id order by c.code")
  List<Course> findRequiring(@Param("id") Long id);
}
