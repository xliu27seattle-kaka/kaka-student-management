package com.kakastudio.sms.section.dto;

import java.util.List;

/** A section with its class list (dropped students left out). */
public record CourseSectionDetail(CourseSectionResponse section, List<RosterEntry> roster) {}
