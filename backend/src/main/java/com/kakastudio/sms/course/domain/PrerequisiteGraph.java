package com.kakastudio.sms.course.domain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The "requires" graph of the catalogue: an edge from a course to each of its prerequisites. Used
 * to refuse a change that would make a course require itself through a chain of other courses.
 */
public final class PrerequisiteGraph {

  private final Map<Long, Collection<Long>> edges;
  private final Map<Long, String> codes;

  public PrerequisiteGraph(Map<Long, ? extends Collection<Long>> edges, Map<Long, String> codes) {
    this.edges = new HashMap<>(edges);
    this.codes = codes;
  }

  public static PrerequisiteGraph of(Collection<Course> courses) {
    Map<Long, Collection<Long>> edges = new HashMap<>();
    Map<Long, String> codes = new HashMap<>();
    for (Course course : courses) {
      // in code order, so the search (and the path it reports) never depends on load order
      edges.put(
          course.getId(),
          course.getPrerequisites().stream()
              .sorted(Comparator.comparing(Course::getCode))
              .map(Course::getId)
              .toList());
      codes.put(course.getId(), course.getCode());
    }
    return new PrerequisiteGraph(edges, codes);
  }

  /**
   * Depth-first search from the proposed prerequisites, looking for a way back to the course.
   *
   * @param courseId the course being changed, or {@code null} for a new course (which nothing
   *     requires yet, so it cannot close a cycle)
   * @return the codes along the cycle, starting and ending with the course, if there is one
   */
  public Optional<List<String>> cycleIf(Long courseId, Collection<Long> prerequisiteIds) {
    if (courseId == null) {
      return Optional.empty();
    }
    Map<Long, Collection<Long>> proposed = new HashMap<>(edges);
    proposed.put(
        courseId,
        prerequisiteIds.stream()
            .sorted(Comparator.comparing((Long id) -> codes.getOrDefault(id, "")))
            .toList());
    Deque<Long> path = new ArrayDeque<>();
    path.addLast(courseId);
    if (leadsBack(courseId, courseId, proposed, path, new HashSet<>())) {
      List<String> named = new ArrayList<>();
      path.forEach(id -> named.add(codes.getOrDefault(id, "#" + id)));
      return Optional.of(named);
    }
    return Optional.empty();
  }

  private static boolean leadsBack(
      Long node,
      Long target,
      Map<Long, Collection<Long>> graph,
      Deque<Long> path,
      Set<Long> visited) {
    for (Long next : graph.getOrDefault(node, List.of())) {
      path.addLast(next);
      if (next.equals(target)) {
        return true;
      }
      if (visited.add(next) && leadsBack(next, target, graph, path, visited)) {
        return true;
      }
      path.removeLast();
    }
    return false;
  }

  public static String describe(List<String> cycle) {
    return String.join(" → ", cycle);
  }
}
