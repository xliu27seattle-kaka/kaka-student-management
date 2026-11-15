package com.kakastudio.sms.transcript.dto;

import java.math.BigDecimal;

public record TranscriptRow(
    String courseCode,
    String courseTitle,
    String sectionCode,
    String professorName,
    int credits,
    RowStatus status,
    Integer score,
    String letter,
    BigDecimal points) {}
