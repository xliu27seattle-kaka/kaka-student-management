package com.kakastudio.sms.course.dto;

import java.util.List;

/** Everything about one course, including what depends on it. */
public record CourseDetail(
    Long id,
    String code,
    String title,
    String description,
    String department,
    int credits,
    boolean active,
    List<CourseRef> prerequisites,
    List<CourseRef> requiredBy,
    long sectionCount) {}
