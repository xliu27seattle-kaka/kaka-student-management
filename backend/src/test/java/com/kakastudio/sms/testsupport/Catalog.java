package com.kakastudio.sms.testsupport;

import com.kakastudio.sms.course.domain.Course;
import com.kakastudio.sms.enrollment.domain.Enrollment;
import com.kakastudio.sms.enrollment.domain.EnrollmentStatus;
import com.kakastudio.sms.professor.domain.Professor;
import com.kakastudio.sms.section.domain.CourseSection;
import com.kakastudio.sms.section.domain.MeetingTime;
import com.kakastudio.sms.semester.domain.Semester;
import com.kakastudio.sms.semester.domain.SemesterStatus;
import com.kakastudio.sms.student.domain.Student;
import com.kakastudio.sms.user.domain.Role;
import com.kakastudio.sms.user.domain.User;
import com.kakastudio.sms.user.domain.UserStatus;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Builds courses, semesters, sections and enrollments for tests. */
public final class Catalog {

  private Catalog() {}

  public static Course course(Long id, String code, int credits, Course... prerequisites) {
    Course course = new Course(code, code + " title", null, code.replaceAll("\\d", ""), credits);
    course.setId(id);
    course.replacePrerequisites(List.of(prerequisites));
    return course;
  }

  public static Semester semester(Long id, String code, SemesterStatus status) {
    Semester semester =
        new Semester(
            code, "Semester " + code, LocalDate.of(2027, 1, 10), LocalDate.of(2027, 5, 10));
    semester.setId(id);
    semester.setStatus(status);
    return semester;
  }

  public static Professor professor(Long id, String lastName) {
    User user =
        Users.user(
            100 + id,
            lastName.toLowerCase() + "@faculty.example",
            Role.PROFESSOR,
            UserStatus.ACTIVE);
    user.setFirstName("Pat");
    user.setLastName(lastName);
    Professor professor = new Professor(user, "P-" + String.format("%04d", id));
    professor.setId(id);
    return professor;
  }

  public static Student student(Long id) {
    User user =
        Users.user(1000 + id, "student" + id + "@uni.example", Role.STUDENT, UserStatus.ACTIVE);
    Student student = new Student(user, "S2026-" + String.format("%04d", id));
    student.setId(id);
    return student;
  }

  public static MeetingTime meeting(DayOfWeek day, String start, String end) {
    return new MeetingTime(day, LocalTime.parse(start), LocalTime.parse(end));
  }

  public static CourseSection section(
      Long id,
      Course course,
      Semester semester,
      Professor professor,
      String letter,
      MeetingTime... meetings) {
    CourseSection section = new CourseSection(course, semester, professor, letter, 20, "Room 101");
    section.setId(id);
    section.replaceMeetings(List.of(meetings));
    return section;
  }

  public static Enrollment enrollment(
      Long id, Student student, CourseSection section, EnrollmentStatus status, Integer score) {
    Enrollment enrollment = new Enrollment(student, section);
    enrollment.setId(id);
    enrollment.setStatus(status);
    enrollment.setScore(score);
    return enrollment;
  }

  public static Enrollment graded(
      Long id, Student student, CourseSection section, int score, String letter, String points) {
    Enrollment enrollment = enrollment(id, student, section, EnrollmentStatus.COMPLETED, score);
    enrollment.setLetterGrade(letter);
    enrollment.setGradePoints(new BigDecimal(points));
    return enrollment;
  }
}
