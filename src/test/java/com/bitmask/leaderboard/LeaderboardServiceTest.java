package com.bitmask.leaderboard;

import com.bitmask.leaderboard.domain.GameKey;
import com.bitmask.leaderboard.dto.LeaderboardEntryDto;
import com.bitmask.leaderboard.domain.LeaderboardType;
import com.bitmask.leaderboard.dto.ScoreRequestDto;
import com.bitmask.leaderboard.error.NotFoundException;
import com.bitmask.leaderboard.service.LeaderboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LeaderboardServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOps;

    private LeaderboardService service;

    // We "freeze" time at Feb 1st, 2026.
    // This is a Sunday, which is Week 05.
    private final Clock fixedClock =
            Clock.fixed(Instant.parse("2026-02-01T10:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setup() {
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        // Manually instantiate to inject the fixed clock
        service = new LeaderboardService(redisTemplate, fixedClock);
    }

    private LocalDate today() {
        return LocalDate.now(fixedClock);
    }

    @Test
    void updateScore_hitsCorrectRedisKeysForDate() {
        ScoreRequestDto request = new ScoreRequestDto("bedwars", "player1", 50.0);

        service.updateScore(request);

        // Verify Global
        verify(zSetOps).incrementScore("leaderboard:bedwars:global", "player1", 50.0);

        // Verify Monthly (Snapshot Feb 2026)
        verify(zSetOps).incrementScore("leaderboard:bedwars:2026-02", "player1", 50.0);

        // Verify Weekly (Week 5 of 2026)
        verify(zSetOps).incrementScore("leaderboard:bedwars:2026-w5", "player1", 50.0);
    }

    @Test
    void getTopPlayers_calculatesRankFromListIndex() {
        GameKey key = new GameKey("bedwars", LeaderboardType.GLOBAL);
        String redisKey = key.toRedisKey(today());

        Set<ZSetOperations.TypedTuple<String>> redisResult = new LinkedHashSet<>();
        redisResult.add(new DefaultTypedTuple<>("pro_gamer", 500.0));
        redisResult.add(new DefaultTypedTuple<>("noob_master", 10.0));

        when(zSetOps.reverseRangeWithScores(redisKey, 0, 9)).thenReturn(redisResult);

        var result = service.getTopPlayers(key, 10);

        assertEquals(2, result.size());

        // Check first player
        assertEquals("pro_gamer", result.get(0).playerId());
        assertEquals(1, result.get(0).rank()); // Index 0 + 1

        // Check second player
        assertEquals("noob_master", result.get(1).playerId());
        assertEquals(2, result.get(1).rank()); // Index 1 + 1
    }

    @Test
    void getPlayerRank_returnsCorrectData() {
        GameKey key = new GameKey("bedwars", LeaderboardType.GLOBAL);
        String player = "active_player";

        when(zSetOps.score(anyString(), eq(player))).thenReturn(150.0);
        when(zSetOps.reverseRank(anyString(), eq(player))).thenReturn(4L);

        LeaderboardEntryDto entry = service.getPlayerRank(key, player);

        assertEquals(150.0, entry.score());
        assertEquals(5, entry.rank());
    }

    @Test
    void getPlayerRank_throwsNotFoundException() {
        GameKey key = new GameKey("bedwars", LeaderboardType.GLOBAL);

        when(zSetOps.score(anyString(), eq("ghost"))).thenReturn(null);

        assertThrows(NotFoundException.class, () -> service.getPlayerRank(key, "ghost"));
    }

    @Test
    void testWithFixedTime() {
        // Uses the 2026-02-01 date from setup
        service.updateScore(new ScoreRequestDto("game", "p1", 10));
        verify(zSetOps).incrementScore(contains("2026-02"), anyString(), anyDouble());
    }

    @Test
    void testWithNewYearsEve() {
        // Locally override the service with a different fixed clock
        Clock newYears = Clock.fixed(Instant.parse("2026-12-31T23:59:59Z"), ZoneOffset.UTC);
        LeaderboardService nyService = new LeaderboardService(redisTemplate, newYears);

        nyService.updateScore(new ScoreRequestDto("game", "p1", 10));

        // Verify it hits the 2026-12 key even though it's almost 2027
        verify(zSetOps).incrementScore(contains("2026-12"), eq("p1"), anyDouble());
    }
}
