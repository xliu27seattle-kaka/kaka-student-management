package com.kakastudio.sms.section.dto;

import com.kakastudio.sms.common.text.Texts;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Creates or edits a section. Capacity defaults to 20 when left out. */
public record SectionRequest(
    @NotNull Long courseId,
    @NotNull Long professorId,
    @Min(1) @Max(200) Integer capacity,
    @Size(max = 40) String room,
    @NotEmpty @Size(max = 6) List<@Valid @NotNull MeetingRequest> meetings) {

  public SectionRequest {
    room = Texts.trim(room);
  }
}
