package com.kakastudio.sms.grade.domain;

import java.math.BigDecimal;

/** One graded attempt as GPA sees it: its credits and the points of its grade. */
public record GradedCredit(int credits, BigDecimal points) {}
