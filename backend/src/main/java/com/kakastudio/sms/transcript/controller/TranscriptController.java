package com.kakastudio.sms.transcript.controller;

import com.kakastudio.sms.student.service.CurrentStudent;
import com.kakastudio.sms.transcript.dto.TranscriptResponse;
import com.kakastudio.sms.transcript.service.TranscriptService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** A student reads their own transcript; an administrator reads anyone's. */
@RestController
public class TranscriptController {

  private final TranscriptService transcriptService;
  private final CurrentStudent currentStudent;

  public TranscriptController(TranscriptService transcriptService, CurrentStudent currentStudent) {
    this.transcriptService = transcriptService;
    this.currentStudent = currentStudent;
  }

  @GetMapping("/api/v1/student/transcript")
  public TranscriptResponse mine() {
    return transcriptService.transcript(currentStudent.id());
  }

  @GetMapping("/api/v1/admin/students/{studentId}/transcript")
  public TranscriptResponse ofStudent(@PathVariable Long studentId) {
    return transcriptService.transcript(studentId);
  }
}
