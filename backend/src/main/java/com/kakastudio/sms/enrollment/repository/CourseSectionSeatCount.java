package com.kakastudio.sms.enrollment.repository;

/** How many seats of a section are taken. */
public record CourseSectionSeatCount(Long sectionId, long taken) {}
