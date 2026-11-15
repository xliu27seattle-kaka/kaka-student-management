package com.kakastudio.sms.course.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A course in the catalogue, such as CS201. What is taught, not when: see {@code Section}. */
@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String code;

  @Column(nullable = false)
  private String title;

  @Column(columnDefinition = "text")
  private String description;

  @Column(nullable = false)
  private String department;

  @Column(nullable = false)
  private int credits;

  /** A retired course keeps its history but gets no new sections. */
  @Column(nullable = false)
  private boolean active = true;

  /** The courses that must be passed before this one. */
  @ManyToMany
  @JoinTable(
      name = "course_prerequisites",
      joinColumns = @JoinColumn(name = "course_id"),
      inverseJoinColumns = @JoinColumn(name = "prerequisite_id"))
  @OrderBy("code")
  @Setter(AccessLevel.NONE)
  private Set<Course> prerequisites = new LinkedHashSet<>();

  public Course(String code, String title, String description, String department, int credits) {
    this.code = code;
    this.title = title;
    this.description = description;
    this.department = department;
    this.credits = credits;
  }

  public void replacePrerequisites(Collection<Course> courses) {
    prerequisites.clear();
    prerequisites.addAll(courses);
  }
}
