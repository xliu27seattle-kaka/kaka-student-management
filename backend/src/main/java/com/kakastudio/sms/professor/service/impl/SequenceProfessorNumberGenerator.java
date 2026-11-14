package com.kakastudio.sms.professor.service.impl;

import com.kakastudio.sms.professor.service.ProfessorNumberGenerator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SequenceProfessorNumberGenerator implements ProfessorNumberGenerator {

  private final JdbcTemplate jdbc;

  public SequenceProfessorNumberGenerator(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public String next() {
    Long sequence = jdbc.queryForObject("select nextval('professor_no_seq')", Long.class);
    return String.format("P-%04d", sequence);
  }
}
