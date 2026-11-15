package com.kakastudio.sms.grade.domain;

import java.math.BigDecimal;

/** A letter grade and the points it is worth on the 4.0 scale. */
public record Grade(String letter, BigDecimal points) {}
