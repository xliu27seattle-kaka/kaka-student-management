package com.kakastudio.sms.course.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PrerequisiteGraphTest {

  // 1 = CS101, 2 = CS201 (needs CS101), 3 = CS301 (needs CS201), 4 = MATH120
  private final PrerequisiteGraph graph =
      new PrerequisiteGraph(
          Map.of(1L, List.of(), 2L, List.of(1L), 3L, List.of(2L), 4L, List.of()),
          Map.of(1L, "CS101", 2L, "CS201", 3L, "CS301", 4L, "MATH120"));

  @Test
  void acceptsPrerequisitesThatDoNotLeadBack() {
    assertThat(graph.cycleIf(3L, List.of(2L, 4L))).isEmpty();
  }

  @Test
  void findsADirectCycleAndNamesThePath() {
    // CS201 requiring CS301, which already requires CS201
    assertThat(graph.cycleIf(2L, List.of(3L))).contains(List.of("CS201", "CS301", "CS201"));
  }

  @Test
  void findsAnIndirectCycle() {
    // CS101 requiring CS301: CS101 -> CS301 -> CS201 -> CS101
    assertThat(graph.cycleIf(1L, List.of(4L, 3L)))
        .contains(List.of("CS101", "CS301", "CS201", "CS101"));
  }

  @Test
  void aNewCourseCannotCloseACycle() {
    assertThat(graph.cycleIf(null, List.of(3L, 1L))).isEmpty();
  }

  @Test
  void formatsThePathWithArrows() {
    assertThat(PrerequisiteGraph.describe(List.of("CS201", "CS301", "CS201")))
        .isEqualTo("CS201 → CS301 → CS201");
  }

  @Test
  void followsPrerequisitesInCodeOrderSoThePathIsAlwaysTheSame() {
    // CS220 and CS301 both lead back to CS101; CS220 comes first by code
    Course cs101 = course(1L, "CS101");
    Course cs201 = course(2L, "CS201");
    Course cs301 = course(3L, "CS301");
    Course cs220 = course(5L, "CS220");
    cs201.replacePrerequisites(java.util.List.of(cs101));
    cs301.replacePrerequisites(java.util.List.of(cs201));
    cs220.replacePrerequisites(java.util.List.of(cs101));
    PrerequisiteGraph built = PrerequisiteGraph.of(java.util.List.of(cs301, cs220, cs201, cs101));

    assertThat(built.cycleIf(1L, java.util.List.of(3L, 5L)))
        .contains(java.util.List.of("CS101", "CS220", "CS101"));
  }

  private static Course course(Long id, String code) {
    Course course = new Course(code, code, null, "CS", 3);
    course.setId(id);
    return course;
  }
}
