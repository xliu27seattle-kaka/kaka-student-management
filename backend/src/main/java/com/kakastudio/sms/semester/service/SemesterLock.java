package com.kakastudio.sms.semester.service;

import com.kakastudio.sms.semester.domain.Semester;

/**
 * Serialises a semester's status change with the work that depends on that status. A transition
 * takes the semester exclusively; enrolling, dropping and grading take it shared, after the student
 * and section locks. So "end the semester" cannot slip between a grade being checked and written,
 * and "back to draft" cannot miss an enrollment that is just committing.
 */
public interface SemesterLock {

  /** Exclusive lock, and the semester's state re-read from the database. */
  void forChange(Semester semester);

  /** Shared lock, and the semester's state re-read from the database. */
  void forUse(Semester semester);
}
