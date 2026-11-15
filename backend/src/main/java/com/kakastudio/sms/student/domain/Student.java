package com.kakastudio.sms.student.domain;

import com.kakastudio.sms.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** The student side of an account: the student number and where they are in their studies. */
@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Student {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @Column(name = "student_no", nullable = false, unique = true, updatable = false)
  private String studentNo;

  @Enumerated(EnumType.STRING)
  @Column(name = "academic_status", nullable = false)
  private AcademicStatus academicStatus = AcademicStatus.ENROLLED;

  /** Issued when graduation is approved. */
  @Column(name = "certificate_no", unique = true)
  private String certificateNo;

  @Column(name = "graduated_at")
  private Instant graduatedAt;

  public Student(User user, String studentNo) {
    this.user = user;
    this.studentNo = studentNo;
  }
}
