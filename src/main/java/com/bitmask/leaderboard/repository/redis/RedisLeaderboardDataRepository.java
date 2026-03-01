package com.bitmask.leaderboard.repository.redis;

import com.bitmask.leaderboard.domain.leaderboard.GameKey;
import com.bitmask.leaderboard.domain.leaderboard.LeaderboardRow;
import com.bitmask.leaderboard.domain.leaderboard.LeaderboardType;
import com.bitmask.leaderboard.domain.ranking.strategy.RankingStrategy;
import com.bitmask.leaderboard.infrastructure.redis.RedisRankingAdapter;
import com.bitmask.leaderboard.repository.LeaderboardDataRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class RedisLeaderboardDataRepository
        implements LeaderboardDataRepository {
    private static final String KEY_PREFIX = "leaderboard:data:";
    private static final String META_PREFIX = ":meta:";

    private final StringRedisTemplate redisTemplate;
    private final RedisRankingAdapter rankingAdapter;

    public RedisLeaderboardDataRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.rankingAdapter = new RedisRankingAdapter(redisTemplate.opsForZSet());
    }

    private String redisKey(GameKey key, LocalDate date) {
        return KEY_PREFIX
                + key.gameId()
                + ":"
                + key.type().getSuffix(date);
    }

    @Override
    public void upsertScore(
            GameKey key,
            LocalDate date,
            String playerId,
            double score,
            Map<String, String> metadata) {
        rankingAdapter.incrementScore(
                redisKey(key, date),
                playerId,
                score);

        if (metadata != null && !metadata.isEmpty()) {
            String metaKey = redisKey(key, date) + META_PREFIX + playerId;
            redisTemplate.opsForHash().putAll(metaKey, metadata);
        }
    }

    @Override
    public List<LeaderboardRow> getTopRange(
            GameKey key,
            RankingStrategy strategy,
            LocalDate date,
            int offset,
            int limit) {
        var range = rankingAdapter.windowWithScores(
                redisKey(key, date),
                strategy,
                offset,
                offset + limit - 1);

        if (range == null || range.isEmpty()) {
            return List.of();
        }

        long rank = offset + 1;
        List<LeaderboardRow> result = new ArrayList<>();

        for (var tuple : range) {
            String metaKey = redisKey(key, date) + META_PREFIX + tuple.getValue();
            Map<Object, Object> rawMeta = redisTemplate.opsForHash().entries(metaKey);
            Map<String, String> metadata = new HashMap<>();
            rawMeta.forEach((k, v) -> metadata.put(k.toString(), v.toString()));

            result.add(new LeaderboardRow(
                    tuple.getValue(),
                    tuple.getScore(),
                    rank++,
                    metadata));
        }

        return result;
    }

    @Override
    public Long getRank(
            GameKey key,
            RankingStrategy strategy,
            LocalDate date,
            String playerId) {
        return rankingAdapter.rank(
                redisKey(key, date),
                strategy,
                playerId);
    }

    @Override
    public Optional<LeaderboardRow> getPlayer(
            GameKey key,
            RankingStrategy strategy,
            LocalDate date,
            String playerId) {
        String redisKey = redisKey(key, date);

        Double score = rankingAdapter.score(redisKey, playerId);
        Long rank = rankingAdapter.rank(redisKey, strategy, playerId);

        if (score == null || rank == null) {
            return Optional.empty();
        }

        String metaKey = redisKey + META_PREFIX + playerId;
        Map<Object, Object> rawMeta = redisTemplate.opsForHash().entries(metaKey);
        Map<String, String> metadata = new HashMap<>();
        rawMeta.forEach((k, v) -> metadata.put(k.toString(), v.toString()));

        return Optional.of(new LeaderboardRow(
                playerId,
                score,
                rank + 1,
                metadata));
    }

    @Override
    public Long getTotalSize(
            GameKey key,
            LocalDate date) {
        Long size = redisTemplate.opsForZSet().zCard(redisKey(key, date));
        return size != null ? size : 0L;
    }

    @Override
    public void deleteLeaderboard(String gameId) {
        LocalDate date = LocalDate.now();
        for (LeaderboardType type : LeaderboardType.values()) {
            redisTemplate.delete(KEY_PREFIX + gameId + ":" + type.getSuffix(date));
        }
    }
}
