package com.kakastudio.sms.professor.repository;

import com.kakastudio.sms.common.text.LikePatterns;
import com.kakastudio.sms.professor.domain.Professor;
import com.kakastudio.sms.user.domain.User;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class ProfessorSpecifications {

  private ProfessorSpecifications() {}

  /**
   * @param query matches professor number, email, first or last name (case-insensitive), or
   *     everything when blank
   */
  public static Specification<Professor> matching(String query) {
    return (root, criteriaQuery, cb) -> {
      if (!StringUtils.hasText(query)) {
        return cb.conjunction();
      }
      Join<Professor, User> user = root.join("user");
      String pattern = LikePatterns.contains(query);
      return cb.or(
          cb.like(cb.lower(root.get("professorNo")), pattern, LikePatterns.ESCAPE),
          cb.like(cb.lower(user.get("email")), pattern, LikePatterns.ESCAPE),
          cb.like(cb.lower(user.get("firstName")), pattern, LikePatterns.ESCAPE),
          cb.like(cb.lower(user.get("lastName")), pattern, LikePatterns.ESCAPE));
    };
  }
}
