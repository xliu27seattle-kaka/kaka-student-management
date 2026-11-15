package com.kakastudio.sms.semester.repository;

import com.kakastudio.sms.semester.domain.Semester;
import com.kakastudio.sms.semester.domain.SemesterStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SemesterRepository extends JpaRepository<Semester, Long> {

  boolean existsByCode(String code);

  List<Semester> findAllByOrderByStartDateDesc();

  List<Semester> findByStatusNotOrderByStartDateDesc(SemesterStatus status);

  List<Semester> findByStatusOrderByStartDateDesc(SemesterStatus status);
}
