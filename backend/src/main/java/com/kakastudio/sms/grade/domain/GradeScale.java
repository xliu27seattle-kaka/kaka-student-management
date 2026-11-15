package com.kakastudio.sms.grade.domain;

/**
 * Turns a score into a grade. Kept behind an interface so another scale can be swapped in without
 * touching grading, transcripts or enrollment rules.
 */
public interface GradeScale {

  /**
   * @param score 0 to 100
   */
  Grade gradeFor(int score);

  /** A passed course earns its credits and satisfies prerequisites. */
  boolean isPassing(int score);
}
