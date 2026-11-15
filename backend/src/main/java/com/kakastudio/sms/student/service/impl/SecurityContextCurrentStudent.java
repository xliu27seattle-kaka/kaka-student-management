package com.kakastudio.sms.student.service.impl;

import com.kakastudio.sms.common.error.ErrorCode;
import com.kakastudio.sms.common.error.NotFoundException;
import com.kakastudio.sms.security.service.CurrentUserProvider;
import com.kakastudio.sms.student.domain.Student;
import com.kakastudio.sms.student.repository.StudentRepository;
import com.kakastudio.sms.student.service.CurrentStudent;
import org.springframework.stereotype.Component;

@Component
public class SecurityContextCurrentStudent implements CurrentStudent {

  private final CurrentUserProvider currentUser;
  private final StudentRepository students;

  public SecurityContextCurrentStudent(
      CurrentUserProvider currentUser, StudentRepository students) {
    this.currentUser = currentUser;
    this.students = students;
  }

  @Override
  public Long id() {
    return students
        .findByUserId(currentUser.require().id())
        .map(Student::getId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.STUDENT_NOT_FOUND, "Student not found"));
  }
}
