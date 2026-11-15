package com.kakastudio.sms.course.mapper;

import com.kakastudio.sms.course.domain.Course;
import com.kakastudio.sms.course.dto.CourseDetail;
import com.kakastudio.sms.course.dto.CourseRef;
import com.kakastudio.sms.course.dto.CourseSummary;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CourseMapper {

  public CourseRef toRef(Course course) {
    return new CourseRef(course.getId(), course.getCode(), course.getTitle());
  }

  public CourseSummary toSummary(Course course) {
    return new CourseSummary(
        course.getId(),
        course.getCode(),
        course.getTitle(),
        course.getDepartment(),
        course.getCredits(),
        course.isActive(),
        refs(course.getPrerequisites()));
  }

  public CourseDetail toDetail(Course course, List<Course> requiredBy, long sectionCount) {
    return new CourseDetail(
        course.getId(),
        course.getCode(),
        course.getTitle(),
        course.getDescription(),
        course.getDepartment(),
        course.getCredits(),
        course.isActive(),
        refs(course.getPrerequisites()),
        refs(requiredBy),
        sectionCount);
  }

  private List<CourseRef> refs(Collection<Course> courses) {
    return courses.stream().map(this::toRef).toList();
  }
}
