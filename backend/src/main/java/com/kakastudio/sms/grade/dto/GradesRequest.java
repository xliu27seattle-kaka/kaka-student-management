package com.kakastudio.sms.grade.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record GradesRequest(@NotNull @Size(max = 200) List<@Valid @NotNull GradeEntry> grades) {}
