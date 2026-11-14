package com.kakastudio.sms.common.paging;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/** Turns the page and size a client sent into a safe page request. */
public final class PageRequests {

  public static final int DEFAULT_SIZE = 20;
  public static final int MAX_SIZE = 100;

  private PageRequests() {}

  /** Newest first (highest id first); a negative page becomes 0 and the size is capped. */
  public static PageRequest newestFirst(int page, int size) {
    int safeSize = size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
    return PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "id"));
  }
}
