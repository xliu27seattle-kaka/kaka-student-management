package com.kakastudio.sms.course.dto;

import java.util.List;

/** A course as one row of the catalogue list. */
public record CourseSummary(
    Long id,
    String code,
    String title,
    String department,
    int credits,
    boolean active,
    List<CourseRef> prerequisites) {}
