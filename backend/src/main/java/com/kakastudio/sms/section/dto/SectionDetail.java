package com.kakastudio.sms.section.dto;

import java.util.List;

/** A section with its class list (dropped students left out). */
public record SectionDetail(SectionResponse section, List<RosterEntry> roster) {}
