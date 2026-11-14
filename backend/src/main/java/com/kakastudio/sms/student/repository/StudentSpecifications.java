package com.kakastudio.sms.student.repository;

import com.kakastudio.sms.common.text.LikePatterns;
import com.kakastudio.sms.student.domain.Student;
import com.kakastudio.sms.user.domain.User;
import com.kakastudio.sms.user.domain.UserStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/** The filters of the student list, built only from the ones that were actually given. */
public final class StudentSpecifications {

  private StudentSpecifications() {}

  /**
   * @param status keep only accounts in this status, or all when {@code null}
   * @param query matches student number, email, first or last name (case-insensitive), or all when
   *     blank
   */
  public static Specification<Student> matching(UserStatus status, String query) {
    return (root, criteriaQuery, cb) -> {
      Join<Student, User> user = root.join("user");
      List<Predicate> predicates = new ArrayList<>();
      if (status != null) {
        predicates.add(cb.equal(user.get("status"), status));
      }
      if (StringUtils.hasText(query)) {
        String pattern = LikePatterns.contains(query);
        predicates.add(
            cb.or(
                cb.like(cb.lower(root.get("studentNo")), pattern, LikePatterns.ESCAPE),
                cb.like(cb.lower(user.get("email")), pattern, LikePatterns.ESCAPE),
                cb.like(cb.lower(user.get("firstName")), pattern, LikePatterns.ESCAPE),
                cb.like(cb.lower(user.get("lastName")), pattern, LikePatterns.ESCAPE)));
      }
      return cb.and(predicates.toArray(Predicate[]::new));
    };
  }
}
