package com.kakastudio.sms.enrollment.dto;

import com.kakastudio.sms.enrollment.domain.EnrollmentStatus;
import com.kakastudio.sms.section.dto.CourseSectionResponse;
import java.util.List;

/**
 * A section as one student sees it on the enrollment page: whether they can take it and, if not,
 * every reason why (the UI shows the first and lists the others in a tooltip).
 *
 * @param myEnrollmentId the student's own row for this section, if they have one
 */
public record Offering(
    CourseSectionResponse section,
    boolean available,
    List<ReasonDto> reasons,
    Long myEnrollmentId,
    EnrollmentStatus myStatus) {}
