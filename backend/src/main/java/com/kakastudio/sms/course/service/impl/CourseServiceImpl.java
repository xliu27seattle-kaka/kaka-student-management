package com.kakastudio.sms.course.service.impl;

import com.kakastudio.sms.common.error.ConflictException;
import com.kakastudio.sms.common.error.ErrorCode;
import com.kakastudio.sms.common.error.NotFoundException;
import com.kakastudio.sms.common.error.RuleViolationException;
import com.kakastudio.sms.common.paging.PageRequests;
import com.kakastudio.sms.common.paging.PageResponse;
import com.kakastudio.sms.course.domain.Course;
import com.kakastudio.sms.course.domain.PrerequisiteGraph;
import com.kakastudio.sms.course.dto.CourseDetail;
import com.kakastudio.sms.course.dto.CourseRequest;
import com.kakastudio.sms.course.dto.CourseSummary;
import com.kakastudio.sms.course.mapper.CourseMapper;
import com.kakastudio.sms.course.repository.CourseRepository;
import com.kakastudio.sms.course.repository.CourseSpecifications;
import com.kakastudio.sms.course.service.CourseService;
import com.kakastudio.sms.section.repository.CourseSectionRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseServiceImpl implements CourseService {

  private final CourseRepository courses;
  private final CourseSectionRepository sections;
  private final CourseMapper mapper;

  public CourseServiceImpl(
      CourseRepository courses, CourseSectionRepository sections, CourseMapper mapper) {
    this.courses = courses;
    this.sections = sections;
    this.mapper = mapper;
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<CourseSummary> list(
      String query, String department, Boolean active, int page, int size) {
    return PageResponse.of(
        courses.findAll(
            CourseSpecifications.matching(query, department, active),
            PageRequests.sorted(page, size, Sort.by("code"))),
        mapper::toSummary);
  }

  @Override
  @Transactional(readOnly = true)
  public CourseDetail get(Long id) {
    return detail(find(id));
  }

  @Override
  @Transactional
  public CourseDetail create(CourseRequest request) {
    if (courses.existsByCode(request.code())) {
      throw codeTaken(request.code());
    }
    Course course =
        new Course(
            request.code(),
            request.title(),
            request.description(),
            request.department(),
            request.credits());
    course.setActive(request.active());
    course.replacePrerequisites(prerequisites(null, request.prerequisiteIds()));
    return detail(courses.save(course));
  }

  @Override
  @Transactional
  public CourseDetail update(Long id, CourseRequest request) {
    Course course = find(id);
    if (!course.getCode().equals(request.code()) && courses.existsByCode(request.code())) {
      throw codeTaken(request.code());
    }
    List<Course> newPrerequisites = prerequisites(id, request.prerequisiteIds());
    course.setCode(request.code());
    course.setTitle(request.title());
    course.setDescription(request.description());
    course.setDepartment(request.department());
    course.setCredits(request.credits());
    course.setActive(request.active());
    course.replacePrerequisites(newPrerequisites);
    return detail(course);
  }

  @Override
  @Transactional
  public void delete(Long id) {
    Course course = find(id);
    List<Course> requiredBy = courses.findRequiring(id);
    if (!requiredBy.isEmpty()) {
      throw new ConflictException(
          ErrorCode.COURSE_IN_USE,
          "Required by "
              + requiredBy.stream().map(Course::getCode).collect(Collectors.joining(", "))
              + ". Remove it from their prerequisites first.");
    }
    if (sections.countByCourseId(id) > 0) {
      throw new ConflictException(
          ErrorCode.COURSE_IN_USE,
          "This course has sections, so it cannot be deleted. Retire it instead.");
    }
    courses.delete(course);
  }

  /** Loads the prerequisites and checks them: they exist, are not the course, form no cycle. */
  private List<Course> prerequisites(Long courseId, List<Long> ids) {
    Set<Long> unique = new LinkedHashSet<>(ids);
    if (courseId != null && unique.contains(courseId)) {
      throw new RuleViolationException(
          ErrorCode.PREREQUISITE_SELF, "A course cannot be its own prerequisite");
    }
    List<Course> found = courses.findAllById(unique);
    if (found.size() != unique.size()) {
      throw new RuleViolationException(
          ErrorCode.PREREQUISITE_NOT_FOUND, "A chosen prerequisite does not exist");
    }
    if (courseId != null && !unique.isEmpty()) {
      Optional<List<String>> cycle =
          PrerequisiteGraph.of(courses.findGraph()).cycleIf(courseId, unique);
      if (cycle.isPresent()) {
        throw new RuleViolationException(
            ErrorCode.PREREQUISITE_CYCLE,
            "These prerequisites would create a cycle: " + PrerequisiteGraph.describe(cycle.get()));
      }
    }
    return found;
  }

  private CourseDetail detail(Course course) {
    long sectionCount = course.getId() == null ? 0 : sections.countByCourseId(course.getId());
    List<Course> requiredBy =
        course.getId() == null ? List.of() : courses.findRequiring(course.getId());
    return mapper.toDetail(course, requiredBy, sectionCount);
  }

  private Course find(Long id) {
    return courses
        .findById(id)
        .orElseThrow(() -> new NotFoundException(ErrorCode.COURSE_NOT_FOUND, "Course not found"));
  }

  private static ConflictException codeTaken(String code) {
    return new ConflictException(ErrorCode.COURSE_CODE_TAKEN, "Course code " + code + " is taken");
  }
}
