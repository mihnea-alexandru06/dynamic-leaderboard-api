package com.bitmask.leaderboard.service;

import com.bitmask.leaderboard.domain.GameKey;
import com.bitmask.leaderboard.dto.LeaderboardEntryDto;
import com.bitmask.leaderboard.domain.LeaderboardType;
import com.bitmask.leaderboard.dto.ScoreRequestDto;
import com.bitmask.leaderboard.error.NotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class LeaderboardService {
    private final StringRedisTemplate redisTemplate;
    private final Clock clock;

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    public void updateScore(ScoreRequestDto request) {

        for (LeaderboardType type : LeaderboardType.values()) {
            GameKey internalKey = new GameKey(request.gameId(), type);

            redisTemplate.opsForZSet().incrementScore(
                    internalKey.toRedisKey(today()),
                    request.playerId(),
                    request.score());
        }
    }

    public List<LeaderboardEntryDto> getTopPlayers(GameKey key, int limit) {
        var range = redisTemplate.opsForZSet()
                .reverseRangeWithScores(key.toRedisKey(today()), 0, limit - 1);

        if (range == null || range.isEmpty()) {
            return List.of();
        }

        List<LeaderboardEntryDto> leaderboard = new ArrayList<>();

        long rank = 1;

        for (var tuple : range) {
            leaderboard.add(new LeaderboardEntryDto(
                    tuple.getValue(),
                    tuple.getScore(),
                    rank++));
        }

        return leaderboard;
    }

    public LeaderboardEntryDto getPlayerRank(GameKey key, String playerId) {
        String redisKey = key.toRedisKey(today());

        Double score = redisTemplate.opsForZSet().score(redisKey, playerId);
        Long rank = redisTemplate.opsForZSet().reverseRank(redisKey, playerId);

        if (score == null || rank == null) {
            throw new NotFoundException(
                    "Player " + playerId + " not found on " + key.type()
                            + " leaderboard for " + key.gameId());
        }

        return new LeaderboardEntryDto(playerId, score, rank + 1);
    }
}
