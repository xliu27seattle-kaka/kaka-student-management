package com.kakastudio.sms.enrollment.domain;

import com.kakastudio.sms.section.domain.Section;
import com.kakastudio.sms.student.domain.Student;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A student's seat in a section, and the grade they got there. One row per student and section. */
@Entity
@Table(name = "enrollments")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Enrollment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "student_id", nullable = false)
  private Student student;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "section_id", nullable = false)
  private Section section;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EnrollmentStatus status = EnrollmentStatus.ENROLLED;

  /** 0 to 100, or {@code null} while ungraded. */
  private Integer score;

  @Column(name = "letter_grade")
  private String letterGrade;

  @Column(name = "grade_points")
  private BigDecimal gradePoints;

  @Column(name = "enrolled_at", nullable = false)
  private Instant enrolledAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public Enrollment(Student student, Section section) {
    this.student = student;
    this.section = section;
  }

  public boolean isEnrolled() {
    return status == EnrollmentStatus.ENROLLED;
  }

  public void clearGrade() {
    score = null;
    letterGrade = null;
    gradePoints = null;
  }

  @PrePersist
  void onCreate() {
    Instant now = Instant.now();
    if (enrolledAt == null) {
      enrolledAt = now;
    }
    updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }
}
