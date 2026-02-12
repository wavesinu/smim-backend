package com.smim.backend.domain.usage;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpHeaders;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Builder
public class RateLimitStatus {
    private int limit;
    private int remaining;
    private int used;
    private boolean unlimited;
    private OffsetDateTime resetAt;

    public HttpHeaders toHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-RateLimit-Limit", String.valueOf(limit));
        headers.add("X-RateLimit-Remaining", String.valueOf(remaining));
        if (resetAt != null) {
            headers.add("X-RateLimit-Reset", resetAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        }
        return headers;
    }
}
