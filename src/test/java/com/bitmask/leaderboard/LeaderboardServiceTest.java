package com.bitmask.leaderboard;

import com.bitmask.leaderboard.domain.leaderboard.GameKey;
import com.bitmask.leaderboard.domain.leaderboard.LeaderboardRow;
import com.bitmask.leaderboard.domain.leaderboard.LeaderboardRuntime;
import com.bitmask.leaderboard.domain.leaderboard.LeaderboardType;
import com.bitmask.leaderboard.domain.ranking.formula.RankingFormula;
import com.bitmask.leaderboard.domain.ranking.strategy.RankingStrategy;
import com.bitmask.leaderboard.domain.ranking.strategy.impl.DescendingRankingStrategy;
import com.bitmask.leaderboard.dto.leaderboard.CreateLeaderboardRequestDto;
import com.bitmask.leaderboard.dto.leaderboard.LeaderboardResponseDto;
import com.bitmask.leaderboard.dto.leaderboard.PlayerLeaderboardDetailsDto;
import com.bitmask.leaderboard.dto.ranking.formula.FormulaDto;
import com.bitmask.leaderboard.dto.ranking.strategy.DescendingStrategyDto;
import com.bitmask.leaderboard.dto.score.ScoreRequestDto;
import com.bitmask.leaderboard.error.exception.NotFoundException;
import com.bitmask.leaderboard.repository.LeaderboardConfigRepository;
import com.bitmask.leaderboard.repository.LeaderboardDataRepository;
import com.bitmask.leaderboard.service.LeaderboardRuntimeProvider;
import com.bitmask.leaderboard.service.LeaderboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LeaderboardServiceTest {

    @Mock
    private LeaderboardDataRepository dataRepository;

    @Mock
    private LeaderboardConfigRepository configRepository;

    @Mock
    private LeaderboardRuntimeProvider runtimeProvider;

    private LeaderboardService service;

    // We "freeze" time at Feb 1st, 2026.
    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-02-01T10:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setup() {
        service = new LeaderboardService(dataRepository, configRepository, runtimeProvider, fixedClock);
    }

    private LocalDate today() {
        return LocalDate.now(fixedClock);
    }

    private List<LeaderboardRow> generateMockRows(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(i -> new LeaderboardRow(
                        "p" + i,
                        1000.0 - (i * 10), // Descending score
                        i,
                        Map.of("color", i % 2 == 0 ? "blue" : "red", "_rawScore",
                                String.valueOf((1000.0 - (i * 10)) + 50))))
                .toList();
    }

    @Test
    void updateScore_hitsAllLeaderboardTypesAndEvaluatesFormula() {
        String gameId = "bedwars";
        ScoreRequestDto request = new ScoreRequestDto("player1", Map.of("kills", 10.0), Map.of("color", "red"));

        RankingStrategy strategy = new DescendingRankingStrategy();
        RankingFormula formula = new RankingFormula("#kills * 2", Set.of("kills"));
        LeaderboardRuntime runtime = new LeaderboardRuntime(strategy, formula, Map.of("season", "1"));

        when(runtimeProvider.get(gameId)).thenReturn(runtime);

        service.updateScore(gameId, request);

        // 10 kills * 2 = 20 score, final strategy score = 20
        Map<String, String> expectedMetadata = Map.of("color", "red", "_rawScore", "20.0");

        for (LeaderboardType type : LeaderboardType.values()) {
            GameKey key = new GameKey(gameId, type);
            verify(dataRepository).upsertScore(eq(key), eq(today()), eq("player1"), eq(20.0), eq(expectedMetadata));
        }
    }

    @Test
    void deleteLeaderboard_success() {
        String gameId = "bedwars";
        when(runtimeProvider.getAllGameIds()).thenReturn(Set.of(gameId, "other"));

        service.deleteLeaderboard(gameId);

        verify(dataRepository).deleteLeaderboard(gameId);
        verify(configRepository).deleteById(gameId);
        verify(runtimeProvider).evict(gameId);
    }

    @Test
    void deleteLeaderboard_notFound() {
        String gameId = "bedwars";
        when(runtimeProvider.getAllGameIds()).thenReturn(Set.of("other"));

        assertThrows(NotFoundException.class, () -> service.deleteLeaderboard(gameId));
    }

    @Test
    void createLeaderboard_success() {
        String gameId = "bedwars";
        CreateLeaderboardRequestDto request = new CreateLeaderboardRequestDto(
                gameId,
                new DescendingStrategyDto(),
                null,
                new FormulaDto("#score", Set.of("score")),
                Map.of("desc", "test leaderboard"));

        when(runtimeProvider.getAllGameIds()).thenReturn(Set.of("other"));

        service.createLeaderboard(request);

        verify(configRepository).save(any());
        verify(runtimeProvider).evict(gameId);
        verify(runtimeProvider).registerNewGame(gameId);
    }

    @Test
    void createLeaderboard_alreadyExists() {
        String gameId = "bedwars";
        CreateLeaderboardRequestDto request = new CreateLeaderboardRequestDto(
                gameId,
                new DescendingStrategyDto(),
                null,
                new FormulaDto("#score", Set.of("score")),
                null);

        when(runtimeProvider.getAllGameIds()).thenReturn(Set.of(gameId));

        assertThrows(IllegalStateException.class, () -> service.createLeaderboard(request));
    }

    @Test
    void getTopPlayers_returnsCorrectResponseWithFilteredMetadata() {
        GameKey key = new GameKey("bedwars", LeaderboardType.GLOBAL);
        RankingStrategy strategy = new DescendingRankingStrategy();
        LeaderboardRuntime runtime = new LeaderboardRuntime(strategy, null,
                Map.of("type", "kills", "_internal", "secret"));

        when(runtimeProvider.get("bedwars")).thenReturn(runtime);

        List<LeaderboardRow> mockRows = generateMockRows(10);
        when(dataRepository.getTopRange(key, strategy, today(), 0, 10)).thenReturn(mockRows);
        when(dataRepository.getTotalSize(key, today())).thenReturn(50L);

        LeaderboardResponseDto response = service.getTopPlayers(key, 0, 10);

        assertEquals("bedwars", response.gameId());
        assertEquals(LeaderboardType.GLOBAL, response.type());
        assertEquals(Map.of("type", "kills"), response.metadata()); // _internal should be filtered
        assertEquals(50, response.pagination().total());
        assertEquals(10, response.entries().size());

        // Check top player
        assertEquals("p1", response.entries().get(0).playerId());
        assertEquals(1040.0, response.entries().get(0).score()); // extracted from _rawScore
        assertEquals(1, response.entries().get(0).rank());
        assertEquals(Map.of("color", "red"), response.entries().get(0).metadata()); // _rawScore filtered

        // Check 10th player
        assertEquals("p10", response.entries().get(9).playerId());
        assertEquals(950.0, response.entries().get(9).score());
        assertEquals(10, response.entries().get(9).rank());
        assertEquals(Map.of("color", "blue"), response.entries().get(9).metadata());
    }

    @Test
    void getPlayerEntry_returnsCorrectDetails() {
        GameKey key = new GameKey("bedwars", LeaderboardType.GLOBAL);
        RankingStrategy strategy = new DescendingRankingStrategy();
        LeaderboardRuntime runtime = new LeaderboardRuntime(strategy, null, Map.of("type", "kills"));

        when(runtimeProvider.get("bedwars")).thenReturn(runtime);

        LeaderboardRow row = generateMockRows(5).get(4); // gets p5
        when(dataRepository.getPlayer(key, strategy, today(), "p5")).thenReturn(Optional.of(row));

        PlayerLeaderboardDetailsDto details = service.getPlayerEntry(key, "p5");

        assertEquals("bedwars", details.gameId());
        assertEquals(LeaderboardType.GLOBAL, details.type());
        assertEquals(Map.of("type", "kills"), details.metadata());
        assertEquals("p5", details.entry().playerId());
        assertEquals(1000.0, details.entry().score()); // extracted from _rawScore
        assertEquals(5, details.entry().rank());
        assertEquals(Map.of("color", "red"), details.entry().metadata());
    }

    @Test
    void getPlayerEntry_notFound() {
        GameKey key = new GameKey("bedwars", LeaderboardType.GLOBAL);
        RankingStrategy strategy = new DescendingRankingStrategy();
        LeaderboardRuntime runtime = new LeaderboardRuntime(strategy, null, Map.of());

        when(runtimeProvider.get("bedwars")).thenReturn(runtime);
        when(dataRepository.getPlayer(key, strategy, today(), "p1")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getPlayerEntry(key, "p1"));
    }

    @Test
    void getPlayerEntries_returnsAllEntries() {
        RankingStrategy strategy = new DescendingRankingStrategy();
        LeaderboardRuntime runtime = new LeaderboardRuntime(strategy, null, Map.of());

        when(runtimeProvider.getAllGameIds()).thenReturn(Set.of("bedwars"));
        when(runtimeProvider.get("bedwars")).thenReturn(runtime);

        GameKey globalKey = new GameKey("bedwars", LeaderboardType.GLOBAL);
        GameKey monthlyKey = new GameKey("bedwars", LeaderboardType.MONTHLY);
        GameKey weeklyKey = new GameKey("bedwars", LeaderboardType.WEEKLY);

        List<LeaderboardRow> mockRows = generateMockRows(2);
        LeaderboardRow globalRow = mockRows.get(0); // Player ID is "p1"
        LeaderboardRow weeklyRow = mockRows.get(0); // Same player for weekly

        when(dataRepository.getPlayer(globalKey, strategy, today(), "p1")).thenReturn(Optional.of(globalRow));
        when(dataRepository.getPlayer(monthlyKey, strategy, today(), "p1")).thenReturn(Optional.empty());
        when(dataRepository.getPlayer(weeklyKey, strategy, today(), "p1")).thenReturn(Optional.of(weeklyRow));

        List<PlayerLeaderboardDetailsDto> entries = service.getPlayerEntries("p1");

        assertEquals(2, entries.size()); // Global and Weekly matched
        boolean hasGlobal = entries.stream().anyMatch(e -> e.type() == LeaderboardType.GLOBAL);
        boolean hasWeekly = entries.stream().anyMatch(e -> e.type() == LeaderboardType.WEEKLY);
        boolean hasMonthly = entries.stream().anyMatch(e -> e.type() == LeaderboardType.MONTHLY);

        assert (hasGlobal);
        assert (hasWeekly);
        assertFalse(hasMonthly);
    }
}
