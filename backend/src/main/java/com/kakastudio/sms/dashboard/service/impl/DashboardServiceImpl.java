package com.kakastudio.sms.dashboard.service.impl;

import com.kakastudio.sms.course.repository.CourseRepository;
import com.kakastudio.sms.dashboard.dto.DashboardResponse;
import com.kakastudio.sms.dashboard.service.DashboardService;
import com.kakastudio.sms.graduation.domain.ApplicationStatus;
import com.kakastudio.sms.graduation.repository.GraduationApplicationRepository;
import com.kakastudio.sms.notification.email.domain.OutboxStatus;
import com.kakastudio.sms.notification.email.repository.EmailOutboxRepository;
import com.kakastudio.sms.semester.service.SemesterService;
import com.kakastudio.sms.student.repository.StudentRepository;
import com.kakastudio.sms.user.domain.Role;
import com.kakastudio.sms.user.domain.UserStatus;
import com.kakastudio.sms.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardServiceImpl implements DashboardService {

  private final StudentRepository students;
  private final UserRepository users;
  private final CourseRepository courses;
  private final GraduationApplicationRepository applications;
  private final EmailOutboxRepository outbox;
  private final SemesterService semesters;

  public DashboardServiceImpl(
      StudentRepository students,
      UserRepository users,
      CourseRepository courses,
      GraduationApplicationRepository applications,
      EmailOutboxRepository outbox,
      SemesterService semesters) {
    this.students = students;
    this.users = users;
    this.courses = courses;
    this.applications = applications;
    this.outbox = outbox;
    this.semesters = semesters;
  }

  @Override
  @Transactional(readOnly = true)
  public DashboardResponse adminSummary() {
    return new DashboardResponse(
        students.countByUserStatus(UserStatus.PENDING),
        students.countByUserStatus(UserStatus.ACTIVE),
        users.countByRoleAndStatus(Role.PROFESSOR, UserStatus.ACTIVE),
        courses.countByActiveTrue(),
        applications.countByStatus(ApplicationStatus.PENDING),
        outbox.countByStatus(OutboxStatus.FAILED),
        semesters.listAll());
  }
}
