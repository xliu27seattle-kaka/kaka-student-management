package com.kakastudio.sms.course.dto;

/** A short reference to a course, for lists of prerequisites. */
public record CourseRef(Long id, String code, String title) {}
