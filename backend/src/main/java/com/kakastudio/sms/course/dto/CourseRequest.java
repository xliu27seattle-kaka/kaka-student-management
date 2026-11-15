package com.kakastudio.sms.course.dto;

import com.kakastudio.sms.common.text.Texts;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Locale;

/**
 * Creates or updates a course. The code and department are stored in upper case without spaces, so
 * "cs 201" and "CS201" are the same course.
 */
public record CourseRequest(
    @NotBlank @Pattern(regexp = "[A-Z]{2,4}[0-9]{3}", message = "must look like CS201") String code,
    @NotBlank @Size(max = 120) String title,
    @Size(max = 2000) String description,
    @NotBlank @Pattern(regexp = "[A-Z]{2,6}", message = "must be 2 to 6 letters") String department,
    @Min(1) @Max(6) Integer credits,
    Boolean active,
    @Size(max = 10) List<@NotNull Long> prerequisiteIds) {

  public static final int DEFAULT_CREDITS = 3;

  public CourseRequest {
    code = code == null ? null : code.replaceAll("\\s", "").toUpperCase(Locale.ROOT);
    department = department == null ? null : department.trim().toUpperCase(Locale.ROOT);
    title = Texts.trim(title);
    description = Texts.trim(description);
    credits = credits == null ? DEFAULT_CREDITS : credits;
    active = active == null ? Boolean.TRUE : active;
    prerequisiteIds = prerequisiteIds == null ? List.of() : List.copyOf(prerequisiteIds);
  }
}
