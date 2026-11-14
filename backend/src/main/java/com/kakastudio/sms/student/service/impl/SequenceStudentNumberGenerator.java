package com.kakastudio.sms.student.service.impl;

import com.kakastudio.sms.student.service.StudentNumberGenerator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Takes the running number from a database sequence, so two registrations never get the same. */
@Component
public class SequenceStudentNumberGenerator implements StudentNumberGenerator {

  private final JdbcTemplate jdbc;

  public SequenceStudentNumberGenerator(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public String next(int year) {
    Long sequence = jdbc.queryForObject("select nextval('student_no_seq')", Long.class);
    return String.format("S%d-%04d", year, sequence);
  }
}
