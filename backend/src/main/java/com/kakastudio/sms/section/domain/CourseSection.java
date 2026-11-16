package com.kakastudio.sms.section.domain;

import com.kakastudio.sms.course.domain.Course;
import com.kakastudio.sms.professor.domain.Professor;
import com.kakastudio.sms.semester.domain.Semester;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One offering of a course in one semester, taught by one professor at fixed weekly times. */
@Entity
@Table(name = "sections")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseSection {

  public static final int DEFAULT_CAPACITY = 20;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "course_id", nullable = false)
  private Course course;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "semester_id", nullable = false)
  private Semester semester;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "professor_id", nullable = false)
  private Professor professor;

  @Column(nullable = false)
  private String letter;

  @Column(nullable = false)
  private int capacity;

  private String room;

  /**
   * Copied from the course when the section is created, so editing the course later never rewrites
   * transcripts.
   */
  @Column(nullable = false)
  private int credits;

  @ElementCollection
  @CollectionTable(name = "section_meetings", joinColumns = @JoinColumn(name = "section_id"))
  @OrderColumn(name = "position")
  @Setter(AccessLevel.NONE)
  private List<MeetingTime> meetings = new ArrayList<>();

  public CourseSection(
      Course course,
      Semester semester,
      Professor professor,
      String letter,
      int capacity,
      String room) {
    this.course = course;
    this.semester = semester;
    this.professor = professor;
    this.letter = letter;
    this.capacity = capacity;
    this.room = room;
    this.credits = course.getCredits();
  }

  /** For example "CS201-A". */
  public String code() {
    return course.getCode() + "-" + letter;
  }

  public void replaceMeetings(Collection<MeetingTime> newMeetings) {
    meetings.clear();
    meetings.addAll(newMeetings);
  }
}
