package com.bitmask.leaderboard.infrastructure.redis;

import com.bitmask.leaderboard.domain.ranking.strategy.RankingStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.Set;

@RequiredArgsConstructor
public final class RedisRankingAdapter {
    private final ZSetOperations<String, String> zset;

    public void addScore(String key, String member, double score) {
        zset.add(key, member, score);
    }

    public void incrementScore(String key, String member, double score) {
        zset.incrementScore(key, member, score);
    }

    public Double score(String key, String member) {
        return zset.score(key, member);
    }

    public Set<String> window(
            String key,
            RankingStrategy strategy,
            long start,
            long end) {
        return strategy.isAscending()
                ? zset.range(key, start, end)
                : zset.reverseRange(key, start, end);
    }

    public Set<ZSetOperations.TypedTuple<String>> windowWithScores(
            String key,
            RankingStrategy strategy,
            long start,
            long end) {
        return strategy.isAscending()
                ? zset.rangeWithScores(key, start, end)
                : zset.reverseRangeWithScores(key, start, end);
    }

    public Long rank(
            String key,
            RankingStrategy strategy,
            String member) {
        return strategy.isAscending()
                ? zset.rank(key, member)
                : zset.reverseRank(key, member);
    }

}
