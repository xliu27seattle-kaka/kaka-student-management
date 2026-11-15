package com.kakastudio.sms.course.repository;

import com.kakastudio.sms.common.text.LikePatterns;
import com.kakastudio.sms.course.domain.Course;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/** The filters of the course list, built only from the ones that were given. */
public final class CourseSpecifications {

  private CourseSpecifications() {}

  public static Specification<Course> matching(String query, String department, Boolean active) {
    return (root, criteriaQuery, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (StringUtils.hasText(query)) {
        String pattern = LikePatterns.contains(query);
        predicates.add(
            cb.or(
                cb.like(cb.lower(root.get("code")), pattern, LikePatterns.ESCAPE),
                cb.like(cb.lower(root.get("title")), pattern, LikePatterns.ESCAPE)));
      }
      if (StringUtils.hasText(department)) {
        predicates.add(
            cb.equal(root.get("department"), department.trim().toUpperCase(Locale.ROOT)));
      }
      if (active != null) {
        predicates.add(cb.equal(root.get("active"), active));
      }
      return cb.and(predicates.toArray(Predicate[]::new));
    };
  }
}
