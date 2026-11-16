package com.kakastudio.sms.section.service.impl;

import com.kakastudio.sms.common.error.ErrorCode;
import com.kakastudio.sms.common.error.NotFoundException;
import com.kakastudio.sms.common.error.RuleViolationException;
import com.kakastudio.sms.course.domain.Course;
import com.kakastudio.sms.course.repository.CourseRepository;
import com.kakastudio.sms.enrollment.domain.EnrollmentStatus;
import com.kakastudio.sms.enrollment.repository.CourseSectionSeatCount;
import com.kakastudio.sms.enrollment.repository.EnrollmentRepository;
import com.kakastudio.sms.notification.email.service.EmailComposer;
import com.kakastudio.sms.notification.message.service.SystemNotices;
import com.kakastudio.sms.professor.domain.Professor;
import com.kakastudio.sms.professor.repository.ProfessorRepository;
import com.kakastudio.sms.section.domain.CourseSection;
import com.kakastudio.sms.section.domain.MeetingTime;
import com.kakastudio.sms.section.dto.CourseSectionDetail;
import com.kakastudio.sms.section.dto.CourseSectionRequest;
import com.kakastudio.sms.section.dto.CourseSectionResponse;
import com.kakastudio.sms.section.dto.MeetingRequest;
import com.kakastudio.sms.section.mapper.CourseSectionMapper;
import com.kakastudio.sms.section.repository.CourseSectionRepository;
import com.kakastudio.sms.section.service.CourseSectionService;
import com.kakastudio.sms.semester.domain.Semester;
import com.kakastudio.sms.semester.domain.SemesterStatus;
import com.kakastudio.sms.semester.repository.SemesterRepository;
import com.kakastudio.sms.user.domain.UserStatus;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseSectionServiceImpl implements CourseSectionService {

  private final CourseSectionRepository sections;
  private final SemesterRepository semesters;
  private final CourseRepository courses;
  private final ProfessorRepository professors;
  private final EnrollmentRepository enrollments;
  private final CourseSectionMapper mapper;
  private final EmailComposer emails;
  private final SystemNotices notices;

  public CourseSectionServiceImpl(
      CourseSectionRepository sections,
      SemesterRepository semesters,
      CourseRepository courses,
      ProfessorRepository professors,
      EnrollmentRepository enrollments,
      CourseSectionMapper mapper,
      EmailComposer emails,
      SystemNotices notices) {
    this.sections = sections;
    this.semesters = semesters;
    this.courses = courses;
    this.professors = professors;
    this.enrollments = enrollments;
    this.mapper = mapper;
    this.emails = emails;
    this.notices = notices;
  }

  @Override
  @Transactional(readOnly = true)
  public List<CourseSectionResponse> listForSemester(Long semesterId) {
    Map<Long, Long> taken =
        enrollments.countSeatsTaken(semesterId, EnrollmentStatus.ENROLLED).stream()
            .collect(
                Collectors.toMap(CourseSectionSeatCount::sectionId, CourseSectionSeatCount::taken));
    return sections.findBySemesterId(semesterId).stream()
        .map(section -> mapper.toResponse(section, taken.getOrDefault(section.getId(), 0L)))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<CourseSectionResponse> listForCourse(Long courseId) {
    return sections.findByCourse(courseId).stream()
        .map(section -> mapper.toResponse(section, enrolled(section.getId())))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public CourseSectionDetail get(Long id) {
    CourseSection section = find(id);
    return mapper.toDetail(section, enrollments.findRoster(id), enrolled(id));
  }

  @Override
  @Transactional
  public CourseSectionResponse create(Long semesterId, CourseSectionRequest request) {
    Semester semester =
        semesters
            .findById(semesterId)
            .orElseThrow(
                () -> new NotFoundException(ErrorCode.SEMESTER_NOT_FOUND, "Semester not found"));
    if (semester.getStatus() != SemesterStatus.DRAFT) {
      throw notEditable("Sections can only be added while the semester is a draft");
    }
    Course course = activeCourse(request.courseId());
    Professor professor = activeProfessor(request.professorId());
    List<MeetingTime> meetings = meetings(request.meetings());
    checkProfessorIsFree(professor, semester, meetings, null);
    CourseSection section =
        new CourseSection(
            course,
            semester,
            professor,
            nextLetter(course.getId(), semesterId),
            Objects.requireNonNullElse(request.capacity(), CourseSection.DEFAULT_CAPACITY),
            request.room());
    section.replaceMeetings(meetings);
    CourseSection saved = sections.save(section);
    tellProfessor(saved);
    return mapper.toResponse(saved, 0);
  }

  @Override
  @Transactional
  public CourseSectionResponse update(Long id, CourseSectionRequest request) {
    // locked as enrolling does, so capacity cannot drop below a seat being taken right now
    CourseSection section =
        sections
            .findWithLockById(id)
            .orElseThrow(
                () -> new NotFoundException(ErrorCode.SECTION_NOT_FOUND, "Section not found"));
    SemesterStatus status = section.getSemester().getStatus();
    if (status == SemesterStatus.STARTED || status == SemesterStatus.ENDED) {
      throw notEditable("Nothing can change once the semester has started");
    }
    List<MeetingTime> meetings = meetings(request.meetings());
    long enrolled = enrolled(id);
    int capacity = Objects.requireNonNullElse(request.capacity(), section.getCapacity());
    if (capacity < enrolled) {
      throw new RuleViolationException(
          ErrorCode.CAPACITY_BELOW_ENROLLED,
          enrolled + " students are enrolled, so the capacity cannot be lower than that");
    }
    boolean scheduleChanged =
        !section.getCourse().getId().equals(request.courseId())
            || !section.getProfessor().getId().equals(request.professorId())
            || !section.getMeetings().equals(meetings);
    if (scheduleChanged) {
      if (status != SemesterStatus.DRAFT) {
        throw notEditable("Once enrollment is open only the capacity and the room can change");
      }
      changeSchedule(section, request, meetings);
    }
    section.setCapacity(capacity);
    section.setRoom(request.room());
    return mapper.toResponse(section, enrolled);
  }

  private void changeSchedule(
      CourseSection section, CourseSectionRequest request, List<MeetingTime> meetings) {
    if (!section.getCourse().getId().equals(request.courseId())) {
      Course course = activeCourse(request.courseId());
      section.setCourse(course);
      section.setCredits(course.getCredits());
      section.setLetter(nextLetter(course.getId(), section.getSemester().getId()));
    }
    Professor professor =
        section.getProfessor().getId().equals(request.professorId())
            ? section.getProfessor()
            : activeProfessor(request.professorId());
    boolean newProfessor = !professor.getId().equals(section.getProfessor().getId());
    checkProfessorIsFree(professor, section.getSemester(), meetings, section.getId());
    section.setProfessor(professor);
    section.replaceMeetings(meetings);
    if (newProfessor) {
      tellProfessor(section);
    }
  }

  @Override
  @Transactional
  public void delete(Long id) {
    CourseSection section = find(id);
    if (section.getSemester().getStatus() != SemesterStatus.DRAFT) {
      throw notEditable("A section can only be deleted while its semester is a draft");
    }
    sections.delete(section);
  }

  @Override
  @Transactional(readOnly = true)
  public List<CourseSectionResponse> currentlyTaughtBy(Long professorId) {
    return sections
        .findByProfessorAndSemesterStatuses(
            professorId, EnumSet.of(SemesterStatus.READY, SemesterStatus.STARTED))
        .stream()
        .map(section -> mapper.toResponse(section, enrolled(section.getId())))
        .toList();
  }

  /** The professor hears about a section they now teach, by email and in their inbox. */
  private void tellProfessor(CourseSection section) {
    String schedule =
        section.getMeetings().stream().map(MeetingTime::label).collect(Collectors.joining(", "))
            + (section.getRoom() == null || section.getRoom().isBlank()
                ? ""
                : " in " + section.getRoom());
    String sectionName = section.code() + " " + section.getCourse().getTitle();
    String semester = section.getSemester().getName();
    notices.send(
        section.getProfessor().getUser(),
        emails.sectionAssigned(section.getProfessor().getUser(), sectionName, semester, schedule),
        "You teach " + sectionName + " in " + semester + ": " + schedule + ".");
  }

  /** Checks each meeting is valid and that the section's own meetings do not collide. */
  private static List<MeetingTime> meetings(List<MeetingRequest> requested) {
    List<MeetingTime> meetings =
        requested.stream().map(m -> new MeetingTime(m.day(), m.start(), m.end())).toList();
    for (MeetingTime meeting : meetings) {
      if (!meeting.isValid()) {
        throw new RuleViolationException(
            ErrorCode.MEETING_INVALID, "A meeting must end after it starts: " + meeting.label());
      }
    }
    for (int i = 0; i < meetings.size(); i++) {
      for (int j = i + 1; j < meetings.size(); j++) {
        if (meetings.get(i).collidesWith(meetings.get(j))) {
          throw new RuleViolationException(
              ErrorCode.MEETING_OVERLAP,
              "Meetings "
                  + meetings.get(i).label()
                  + " and "
                  + meetings.get(j).label()
                  + " need at least "
                  + MeetingTime.MIN_GAP_MINUTES
                  + " minutes between them");
        }
      }
    }
    return meetings;
  }

  private void checkProfessorIsFree(
      Professor professor, Semester semester, List<MeetingTime> meetings, Long ignoredSectionId) {
    for (CourseSection other :
        sections.findByProfessorIdAndSemesterId(professor.getId(), semester.getId())) {
      if (other.getId().equals(ignoredSectionId)) {
        continue;
      }
      for (MeetingTime mine : meetings) {
        for (MeetingTime theirs : other.getMeetings()) {
          if (mine.collidesWith(theirs)) {
            throw new RuleViolationException(
                ErrorCode.PROFESSOR_SCHEDULE_CONFLICT,
                professor.getUser().fullName()
                    + " already teaches "
                    + other.code()
                    + " at "
                    + theirs.label()
                    + "; classes need at least "
                    + MeetingTime.MIN_GAP_MINUTES
                    + " minutes between them");
          }
        }
      }
    }
  }

  private String nextLetter(Long courseId, Long semesterId) {
    List<String> used = sections.findLetters(courseId, semesterId);
    for (char letter = 'A'; letter <= 'Z'; letter++) {
      if (!used.contains(String.valueOf(letter))) {
        return String.valueOf(letter);
      }
    }
    throw new RuleViolationException(
        ErrorCode.SECTION_NOT_EDITABLE, "This course already has 26 sections this semester");
  }

  private Course activeCourse(Long id) {
    Course course =
        courses
            .findById(id)
            .orElseThrow(
                () -> new NotFoundException(ErrorCode.COURSE_NOT_FOUND, "Course not found"));
    if (!course.isActive()) {
      throw new RuleViolationException(
          ErrorCode.COURSE_INACTIVE, course.getCode() + " is retired and gets no new sections");
    }
    return course;
  }

  private Professor activeProfessor(Long id) {
    Professor professor =
        professors
            .findById(id)
            .orElseThrow(
                () -> new NotFoundException(ErrorCode.PROFESSOR_NOT_FOUND, "Professor not found"));
    if (professor.getUser().getStatus() != UserStatus.ACTIVE) {
      throw new RuleViolationException(
          ErrorCode.PROFESSOR_INACTIVE, professor.getUser().fullName() + " is not active");
    }
    return professor;
  }

  private long enrolled(Long sectionId) {
    return enrollments.countBySectionIdAndStatus(sectionId, EnrollmentStatus.ENROLLED);
  }

  private CourseSection find(Long id) {
    return sections
        .findById(id)
        .orElseThrow(() -> new NotFoundException(ErrorCode.SECTION_NOT_FOUND, "Section not found"));
  }

  private static RuleViolationException notEditable(String message) {
    return new RuleViolationException(ErrorCode.SECTION_NOT_EDITABLE, message);
  }
}
