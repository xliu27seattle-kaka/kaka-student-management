package com.kakastudio.sms.enrollment.repository;

/** How many seats of a section are taken. */
public record SectionSeatCount(Long sectionId, long taken) {}
