package com.smim.backend.domain.usage;

import com.smim.backend.domain.user.Role;
import com.smim.backend.domain.user.User;
import com.smim.backend.domain.user.UserRepository;
import com.smim.backend.global.error.ErrorCode;
import com.smim.backend.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private static final int USER_DAILY_LIMIT = 5;
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RedisTemplate<String, String> redisTemplate;
    private final UserRepository userRepository;

    private final Map<String, LocalCounter> localCounters = new ConcurrentHashMap<>();

    public RateLimitStatus consume(Long userId) {
        User user = getUserOrThrow(userId);
        if (user.getRole() == Role.ADMIN) {
            return buildUnlimitedStatus();
        }

        OffsetDateTime resetAt = nextReset();
        int used = incrementUsage(userId, resetAt);
        if (used > USER_DAILY_LIMIT) {
            throw new BusinessException(ErrorCode.DAILY_LIMIT_EXCEEDED);
        }
        return buildStatus(USER_DAILY_LIMIT, used, resetAt, false);
    }

    public RateLimitStatus getStatus(Long userId) {
        User user = getUserOrThrow(userId);
        if (user.getRole() == Role.ADMIN) {
            return buildUnlimitedStatus();
        }

        OffsetDateTime resetAt = nextReset();
        int used = getUsage(userId, resetAt);
        return buildStatus(USER_DAILY_LIMIT, used, resetAt, false);
    }

    private int incrementUsage(Long userId, OffsetDateTime resetAt) {
        String key = buildKey(userId);
        try {
            Long value = redisTemplate.opsForValue().increment(key);
            if (value != null && value == 1L) {
                redisTemplate.expire(key, Duration.between(OffsetDateTime.now(DEFAULT_ZONE), resetAt));
            }
            return value == null ? 0 : value.intValue();
        } catch (Exception ex) {
            log.warn("Redis 사용량 증가 실패. 로컬 카운터로 대체합니다.", ex);
            return incrementLocal(key, resetAt);
        }
    }

    private int getUsage(Long userId, OffsetDateTime resetAt) {
        String key = buildKey(userId);
        try {
            String value = redisTemplate.opsForValue().get(key);
            return value == null ? 0 : Integer.parseInt(value);
        } catch (Exception ex) {
            log.warn("Redis 사용량 조회 실패. 로컬 카운터로 대체합니다.", ex);
            return getLocal(key, resetAt);
        }
    }

    private int incrementLocal(String key, OffsetDateTime resetAt) {
        LocalCounter counter = localCounters.compute(key, (ignored, current) -> {
            if (current == null || current.isExpired()) {
                return new LocalCounter(resetAt);
            }
            return current;
        });
        return counter.increment();
    }

    private int getLocal(String key, OffsetDateTime resetAt) {
        LocalCounter counter = localCounters.compute(key, (ignored, current) -> {
            if (current == null || current.isExpired()) {
                return new LocalCounter(resetAt);
            }
            return current;
        });
        return counter.get();
    }

    private RateLimitStatus buildStatus(int limit, int used, OffsetDateTime resetAt, boolean unlimited) {
        int remaining = Math.max(0, limit - used);
        return RateLimitStatus.builder()
                .limit(limit)
                .remaining(remaining)
                .used(used)
                .unlimited(unlimited)
                .resetAt(resetAt)
                .build();
    }

    private RateLimitStatus buildUnlimitedStatus() {
        return RateLimitStatus.builder()
                .limit(-1)
                .remaining(-1)
                .used(0)
                .unlimited(true)
                .resetAt(nextReset())
                .build();
    }

    private OffsetDateTime nextReset() {
        ZonedDateTime now = ZonedDateTime.now(DEFAULT_ZONE);
        ZonedDateTime reset = now.toLocalDate().plusDays(1).atStartOfDay(DEFAULT_ZONE);
        return reset.toOffsetDateTime();
    }

    private String buildKey(Long userId) {
        LocalDate date = LocalDate.now(DEFAULT_ZONE);
        return "usage:" + userId + ":" + date.format(DATE_FORMATTER);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private static class LocalCounter {
        private final OffsetDateTime resetAt;
        private final AtomicInteger count = new AtomicInteger(0);

        private LocalCounter(OffsetDateTime resetAt) {
            this.resetAt = resetAt;
        }

        private boolean isExpired() {
            return OffsetDateTime.now(DEFAULT_ZONE).isAfter(resetAt);
        }

        private int increment() {
            return count.incrementAndGet();
        }

        private int get() {
            return count.get();
        }
    }
}
