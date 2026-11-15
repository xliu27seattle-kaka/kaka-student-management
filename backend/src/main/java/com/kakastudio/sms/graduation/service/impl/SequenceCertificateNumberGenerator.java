package com.kakastudio.sms.graduation.service.impl;

import com.kakastudio.sms.graduation.service.CertificateNumberGenerator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SequenceCertificateNumberGenerator implements CertificateNumberGenerator {

  private final JdbcTemplate jdbc;

  public SequenceCertificateNumberGenerator(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public String next(int year) {
    Long sequence = jdbc.queryForObject("select nextval('certificate_no_seq')", Long.class);
    return String.format("KS-%d-%04d", year, sequence);
  }
}
