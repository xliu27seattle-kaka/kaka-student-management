package com.kakastudio.sms.course.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kakastudio.sms.common.error.ApiException;
import com.kakastudio.sms.common.error.ErrorCode;
import com.kakastudio.sms.course.domain.Course;
import com.kakastudio.sms.course.dto.CourseDetail;
import com.kakastudio.sms.course.dto.CourseRequest;
import com.kakastudio.sms.course.mapper.CourseMapper;
import com.kakastudio.sms.course.repository.CourseRepository;
import com.kakastudio.sms.section.repository.CourseSectionRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class CourseServiceImplTest {

  private final CourseRepository courses = mock(CourseRepository.class);
  private final CourseSectionRepository sections = mock(CourseSectionRepository.class);
  private CourseServiceImpl service;

  private Course cs101;
  private Course cs201;
  private Course cs301;

  @BeforeEach
  void setUp() {
    cs101 = course(1L, "CS101");
    cs201 = course(2L, "CS201");
    cs301 = course(3L, "CS301");
    cs201.replacePrerequisites(List.of(cs101));
    cs301.replacePrerequisites(List.of(cs201));
    when(courses.findById(1L)).thenReturn(Optional.of(cs101));
    when(courses.findById(2L)).thenReturn(Optional.of(cs201));
    when(courses.findById(3L)).thenReturn(Optional.of(cs301));
    when(courses.findAllById(any())).thenAnswer(call -> byIds(call.getArgument(0)));
    when(courses.findGraph()).thenReturn(List.of(cs101, cs201, cs301));
    when(courses.save(any())).thenAnswer(call -> call.getArgument(0));
    service = new CourseServiceImpl(courses, sections, new CourseMapper());
  }

  private List<Course> byIds(Iterable<Long> ids) {
    List<Course> all = List.of(cs101, cs201, cs301);
    List<Long> wanted = new java.util.ArrayList<>();
    ids.forEach(wanted::add);
    return all.stream().filter(c -> wanted.contains(c.getId())).toList();
  }

  private static Course course(Long id, String code) {
    Course course = new Course(code, code + " title", null, "CS", 3);
    course.setId(id);
    return course;
  }

  private static CourseRequest request(String code, List<Long> prerequisites) {
    return new CourseRequest(code, "Algorithms", "About algorithms", "cs", 3, true, prerequisites);
  }

  @Test
  void createsACourseWithItsPrerequisites() {
    when(courses.existsByCode("CS250")).thenReturn(false);

    CourseDetail created = service.create(request("cs250", List.of(2L)));

    assertThat(created.code()).isEqualTo("CS250");
    assertThat(created.department()).isEqualTo("CS");
    assertThat(created.prerequisites()).extracting("code").containsExactly("CS201");
  }

  @Test
  void refusesADuplicateCode() {
    when(courses.existsByCode("CS201")).thenReturn(true);

    ApiException error =
        catchThrowableOfType(ApiException.class, () -> service.create(request("CS201", List.of())));

    assertThat(error.code()).isEqualTo(ErrorCode.COURSE_CODE_TAKEN);
    assertThat(error.status()).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void refusesAnUnknownPrerequisite() {
    when(courses.existsByCode("CS250")).thenReturn(false);

    ApiException error =
        catchThrowableOfType(
            ApiException.class, () -> service.create(request("CS250", List.of(2L, 99L))));

    assertThat(error.code()).isEqualTo(ErrorCode.PREREQUISITE_NOT_FOUND);
  }

  @Test
  void refusesACourseAsItsOwnPrerequisite() {
    ApiException error =
        catchThrowableOfType(
            ApiException.class, () -> service.update(2L, request("CS201", List.of(2L))));

    assertThat(error.code()).isEqualTo(ErrorCode.PREREQUISITE_SELF);
  }

  @Test
  void refusesACycleAndNamesThePath() {
    ApiException error =
        catchThrowableOfType(
            ApiException.class, () -> service.update(1L, request("CS101", List.of(3L))));

    assertThat(error.code()).isEqualTo(ErrorCode.PREREQUISITE_CYCLE);
    assertThat(error.getMessage()).contains("CS101 → CS301 → CS201 → CS101");
    assertThat(cs101.getPrerequisites()).isEmpty();
  }

  @Test
  void refusesToDeleteACourseOthersRequire() {
    when(courses.findRequiring(1L)).thenReturn(List.of(cs201));

    ApiException error = catchThrowableOfType(ApiException.class, () -> service.delete(1L));

    assertThat(error.code()).isEqualTo(ErrorCode.COURSE_IN_USE);
    assertThat(error.getMessage()).contains("CS201");
    verify(courses, never()).delete(any(Course.class));
  }

  @Test
  void refusesToDeleteACourseWithSections() {
    when(courses.findRequiring(3L)).thenReturn(List.of());
    when(sections.countByCourseId(3L)).thenReturn(2L);

    ApiException error = catchThrowableOfType(ApiException.class, () -> service.delete(3L));

    assertThat(error.code()).isEqualTo(ErrorCode.COURSE_IN_USE);
    verify(courses, never()).delete(any(Course.class));
  }

  @Test
  void deletesAnUnusedCourse() {
    when(courses.findRequiring(3L)).thenReturn(List.of());
    when(sections.countByCourseId(3L)).thenReturn(0L);

    service.delete(3L);

    verify(courses).delete(cs301);
  }
}
