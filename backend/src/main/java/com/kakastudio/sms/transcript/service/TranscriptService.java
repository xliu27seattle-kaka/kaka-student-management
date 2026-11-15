package com.kakastudio.sms.transcript.service;

import com.kakastudio.sms.transcript.dto.TranscriptResponse;

public interface TranscriptService {

  TranscriptResponse transcript(Long studentId);

  /** Credits of the distinct courses the student has passed. */
  int earnedCredits(Long studentId);
}
