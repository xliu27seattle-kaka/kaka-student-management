package com.kakastudio.sms.professor.domain;

import com.kakastudio.sms.user.domain.User;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** The professor side of an account: number, title and the public self-introduction. */
@Entity
@Table(name = "professors")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Professor {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @Column(name = "professor_no", nullable = false, unique = true, updatable = false)
  private String professorNo;

  private String title;

  @Column(columnDefinition = "text")
  private String bio;

  /** File name of the portrait, served by the frontend from {@code /assets/professors/}. */
  @Column(name = "photo_path")
  private String photoPath;

  @ElementCollection
  @CollectionTable(name = "professor_expertise", joinColumns = @JoinColumn(name = "professor_id"))
  @OrderColumn(name = "position")
  @Column(name = "label", nullable = false)
  @Setter(AccessLevel.NONE)
  private List<String> expertise = new ArrayList<>();

  @ElementCollection
  @CollectionTable(name = "professor_experience", joinColumns = @JoinColumn(name = "professor_id"))
  @OrderColumn(name = "position")
  @Setter(AccessLevel.NONE)
  private List<ExperienceEntry> experience = new ArrayList<>();

  @ElementCollection
  @CollectionTable(name = "professor_awards", joinColumns = @JoinColumn(name = "professor_id"))
  @OrderColumn(name = "position")
  @Setter(AccessLevel.NONE)
  private List<AwardEntry> awards = new ArrayList<>();

  public Professor(User user, String professorNo) {
    this.user = user;
    this.professorNo = professorNo;
  }

  /** Replaces the three lists, keeping the order given. */
  public void replaceLists(
      List<String> newExpertise, List<ExperienceEntry> newExperience, List<AwardEntry> newAwards) {
    expertise.clear();
    expertise.addAll(newExpertise);
    experience.clear();
    experience.addAll(newExperience);
    awards.clear();
    awards.addAll(newAwards);
  }
}
