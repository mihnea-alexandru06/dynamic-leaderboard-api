package com.bitmask.leaderboard.repository.redis;

import com.bitmask.leaderboard.domain.leaderboard.LeaderboardConfiguration;
import com.bitmask.leaderboard.repository.LeaderboardConfigRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
@AllArgsConstructor
public class RedisLeaderboardConfigRepository
        implements LeaderboardConfigRepository {
    private static final String KEY_PREFIX = "leaderboard:config:";
    private static final String INDEX_KEY = "leaderboard:configIds:";

    private final StringRedisTemplate stringTemplate;
    private final RedisTemplate<String, LeaderboardConfiguration> redisTemplate;

    @Override
    public void save(LeaderboardConfiguration config) {
        redisTemplate.opsForValue()
                .set(KEY_PREFIX + config.gameId(), config);

        stringTemplate.opsForSet()
                .add(INDEX_KEY, config.gameId());
    }

    @Override
    public Optional<LeaderboardConfiguration> findById(String gameId) {
        return Optional.ofNullable(
                redisTemplate.opsForValue().get(KEY_PREFIX + gameId));
    }

    @Override
    public Set<String> getAllGameIds() {
        Set<String> ids = stringTemplate.opsForSet().members(INDEX_KEY);
        return ids == null ? Set.of() : ids;
    }

    @Override
    public void deleteById(String gameId) {
        redisTemplate.delete(KEY_PREFIX + gameId);
        stringTemplate.opsForSet().remove(INDEX_KEY, gameId);
    }
}
