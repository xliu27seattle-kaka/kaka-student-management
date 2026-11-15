package com.kakastudio.sms.semester.service.impl;

import com.kakastudio.sms.semester.domain.Semester;
import com.kakastudio.sms.semester.service.SemesterLock;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Component;

/**
 * Row locks on the semester: {@code SELECT ... FOR UPDATE} to change it, {@code FOR SHARE} to rely
 * on it. Refreshing with the lock also reloads the status, so the caller never acts on a value read
 * before the lock was granted. Must be called inside a transaction.
 */
@Component
public class RowSemesterLock implements SemesterLock {

  private final EntityManager entityManager;

  public RowSemesterLock(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Override
  public void forChange(Semester semester) {
    entityManager.refresh(semester, LockModeType.PESSIMISTIC_WRITE);
  }

  @Override
  public void forUse(Semester semester) {
    entityManager.refresh(semester, LockModeType.PESSIMISTIC_READ);
  }
}
