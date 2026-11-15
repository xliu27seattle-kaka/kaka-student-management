package com.kakastudio.sms.semester.domain;

import static com.kakastudio.sms.semester.domain.SemesterStatus.DRAFT;
import static com.kakastudio.sms.semester.domain.SemesterStatus.ENDED;
import static com.kakastudio.sms.semester.domain.SemesterStatus.READY;
import static com.kakastudio.sms.semester.domain.SemesterStatus.STARTED;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SemesterStatusTest {

  @Test
  void allowsOnlyTheDocumentedTransitions() {
    assertThat(DRAFT.canMoveTo(READY)).isTrue();
    assertThat(READY.canMoveTo(STARTED)).isTrue();
    assertThat(READY.canMoveTo(DRAFT)).isTrue();
    assertThat(STARTED.canMoveTo(ENDED)).isTrue();

    assertThat(DRAFT.canMoveTo(STARTED)).isFalse();
    assertThat(STARTED.canMoveTo(READY)).isFalse();
    assertThat(ENDED.canMoveTo(STARTED)).isFalse();
    assertThat(READY.canMoveTo(READY)).isFalse();
  }

  @Test
  void draftIsHiddenFromStudentsAndProfessors() {
    assertThat(DRAFT.isVisibleToMembers()).isFalse();
    assertThat(READY.isVisibleToMembers()).isTrue();
  }
}
