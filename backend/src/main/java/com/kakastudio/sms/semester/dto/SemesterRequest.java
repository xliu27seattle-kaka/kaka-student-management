package com.kakastudio.sms.semester.dto;

import com.kakastudio.sms.common.text.Texts;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Locale;

/** Creates or edits a semester. The code is the year and a term: SP, SU or FA. */
public record SemesterRequest(
    @NotBlank @Pattern(regexp = "[0-9]{4}(SP|SU|FA)", message = "must look like 2027SP")
        String code,
    @NotBlank @Size(max = 60) String name,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate) {

  public SemesterRequest {
    code = code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    name = Texts.trim(name);
  }
}
