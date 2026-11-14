package com.kakastudio.sms.student.mapper;

import com.kakastudio.sms.student.domain.Student;
import com.kakastudio.sms.student.dto.StudentResponse;
import com.kakastudio.sms.user.domain.User;
import org.springframework.stereotype.Component;

@Component
public class StudentMapper {

  public StudentResponse toResponse(Student student) {
    User user = student.getUser();
    return new StudentResponse(
        student.getId(),
        student.getStudentNo(),
        user.getEmail(),
        user.getFirstName(),
        user.getLastName(),
        user.getStatus(),
        student.getAcademicStatus(),
        user.getCreatedAt());
  }
}
