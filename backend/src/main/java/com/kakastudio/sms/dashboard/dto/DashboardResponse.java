package com.kakastudio.sms.dashboard.dto;

import com.kakastudio.sms.semester.dto.SemesterResponse;
import java.util.List;

/** The numbers on the administrator's home page; the pending ones are things to act on. */
public record DashboardResponse(
    long pendingStudents,
    long activeStudents,
    long activeProfessors,
    long activeCourses,
    long pendingGraduations,
    long failedEmails,
    List<SemesterResponse> semesters) {}
