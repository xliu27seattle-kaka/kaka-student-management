package com.kakastudio.sms.section.dto;

import com.kakastudio.sms.semester.domain.SemesterStatus;
import java.util.List;

public record SectionResponse(
    Long id,
    String code,
    String letter,
    Long courseId,
    String courseCode,
    String courseTitle,
    int credits,
    Long semesterId,
    String semesterCode,
    String semesterName,
    SemesterStatus semesterStatus,
    Long professorId,
    String professorName,
    int capacity,
    long enrolledCount,
    String room,
    List<MeetingDto> meetings) {}
