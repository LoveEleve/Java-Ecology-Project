package com.loveeleve.ecology.redis.service;

import com.loveeleve.ecology.redis.config.RedisKeys;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class LeaderboardService {

    private final StringRedisTemplate redisTemplate;

    public LeaderboardService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void add(String board, String member, double score) {
        redisTemplate.opsForZSet().add(RedisKeys.leaderboard(board), member, score);
    }

    public List<RankEntry> top(String board, int count) {
        int safeCount = Math.max(1, Math.min(count, 100));
        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores(RedisKeys.leaderboard(board), 0, safeCount - 1L);
        if (tuples == null) {
            return List.of();
        }
        return tuples.stream()
                .map(tuple -> new RankEntry(tuple.getValue(), tuple.getScore()))
                .toList();
    }

    public record RankEntry(String member, Double score) {
    }
}
