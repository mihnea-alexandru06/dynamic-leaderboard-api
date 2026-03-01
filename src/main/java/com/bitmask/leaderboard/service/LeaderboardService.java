package com.bitmask.leaderboard.service;

import com.bitmask.leaderboard.domain.leaderboard.GameKey;
import com.bitmask.leaderboard.domain.leaderboard.LeaderboardConfiguration;
import com.bitmask.leaderboard.domain.leaderboard.LeaderboardRow;
import com.bitmask.leaderboard.domain.leaderboard.LeaderboardRuntime;
import com.bitmask.leaderboard.domain.leaderboard.LeaderboardType;
import com.bitmask.leaderboard.dto.leaderboard.CreateLeaderboardRequestDto;
import com.bitmask.leaderboard.dto.leaderboard.LeaderboardEntryDto;
import com.bitmask.leaderboard.dto.leaderboard.LeaderboardResponseDto;
import com.bitmask.leaderboard.dto.leaderboard.PaginationDto;
import com.bitmask.leaderboard.dto.leaderboard.PlayerLeaderboardDetailsDto;
import com.bitmask.leaderboard.dto.ranking.formula.FormulaDto;
import com.bitmask.leaderboard.dto.score.ScoreRequestDto;
import com.bitmask.leaderboard.error.exception.NotFoundException;
import com.bitmask.leaderboard.repository.LeaderboardConfigRepository;
import com.bitmask.leaderboard.repository.LeaderboardDataRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class LeaderboardService {
    private static final String META_RAW_SCORE = "_rawScore";

    private final LeaderboardDataRepository dataRepository;
    private final LeaderboardConfigRepository configRepository;
    private final LeaderboardRuntimeProvider runtimeProvider;
    private final Clock clock;


    private LocalDate today() {
        return LocalDate.now(clock);
    }

    public void updateScore(String gameId, ScoreRequestDto request) {
        LocalDate date = today();
        LeaderboardRuntime runtime = runtimeProvider.get(gameId);

        double rawScore = runtime.formula().evaluate(request.values());
        double finalScore = runtime.strategy().score(rawScore);

        Map<String, String> metadata = request.metadata() != null
                ? new HashMap<>(request.metadata())
                : new HashMap<>();
        metadata.put(META_RAW_SCORE, String.valueOf(rawScore));

        for (LeaderboardType type : LeaderboardType.values()) {
            GameKey key = new GameKey(gameId, type);
            dataRepository.upsertScore(
                    key,
                    date,
                    request.playerId(),
                    finalScore,
                    metadata);
        }
    }

    public void deleteLeaderboard(String gameId) {
        if (!runtimeProvider.getAllGameIds().contains(gameId)) {
            throw new NotFoundException("Leaderboard '" + gameId + "' not found");
        }

        dataRepository.deleteLeaderboard(gameId);
        configRepository.deleteById(gameId);
        runtimeProvider.evict(gameId);
    }

    public void createLeaderboard(CreateLeaderboardRequestDto request) {
        if (runtimeProvider.getAllGameIds().contains(request.gameId())) {
            throw new IllegalStateException("Leaderboard already exists");
        }

        FormulaDto formula = request.formula();

        LeaderboardConfiguration configuration = new LeaderboardConfiguration(
                request.gameId(),
                request.rankingStrategy(),
                formula,
                request.metadata());

        configRepository.save(configuration);
        runtimeProvider.evict(request.gameId());
        runtimeProvider.registerNewGame(request.gameId());
    }

    public LeaderboardResponseDto getTopPlayers(GameKey key, int offset, int limit) {
        LeaderboardRuntime runtime = runtimeProvider.get(key.gameId());
        Map<String, String> metadata = runtime.metadata();

        List<LeaderboardEntryDto> entries = dataRepository.getTopRange(
                        key,
                        runtime.strategy(),
                        today(),
                        offset,
                        limit).stream()
                .map(this::toEntry)
                .toList();

        Long totalSize = dataRepository.getTotalSize(key, today());

        return new LeaderboardResponseDto(
                key.gameId(),
                key.type(),
                filterInternalMetadata(metadata),
                new PaginationDto(offset, limit, entries.size(), totalSize.intValue()),
                entries);
    }

    public PlayerLeaderboardDetailsDto getPlayerEntry(GameKey key, String playerId) {
        LeaderboardRuntime runtime = runtimeProvider.get(key.gameId());
        Map<String, String> metadata = runtime.metadata();

        return dataRepository
                .getPlayer(key, runtime.strategy(), today(), playerId)
                .map(row -> new PlayerLeaderboardDetailsDto(
                        key.gameId(),
                        key.type(),
                        filterInternalMetadata(metadata),
                        toEntry(row)
                ))
                .orElseThrow(() -> new NotFoundException(
                        "Player " + playerId + " not found"));
    }

    public List<PlayerLeaderboardDetailsDto> getPlayerEntries(String playerId) {
        LocalDate date = today();
        List<PlayerLeaderboardDetailsDto> result = new ArrayList<>();

        for (String gameId : runtimeProvider.getAllGameIds()) {
            LeaderboardRuntime runtime = runtimeProvider.get(gameId);
            Map<String, String> metadata = runtime.metadata();

            for (LeaderboardType type : LeaderboardType.values()) {
                GameKey key = new GameKey(gameId, type);

                dataRepository.getPlayer(key, runtime.strategy(), date, playerId)
                        .ifPresent(row -> result.add(
                                new PlayerLeaderboardDetailsDto(
                                        key.gameId(),
                                        key.type(),
                                        filterInternalMetadata(metadata),
                                        toEntry(row))
                        ));
            }
        }

        return result;
    }

    private Map<String, String> filterInternalMetadata(Map<String, String> metadata) {
        if (metadata == null)
            return Map.of();

        return metadata.entrySet().stream()
                .filter(e -> !e.getKey().startsWith("_"))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private double extractRawScore(Map<String, String> metadata, double fallbackScore) {
        if (metadata != null && metadata.containsKey(META_RAW_SCORE)) {
            try {
                return Double.parseDouble(metadata.get(META_RAW_SCORE));
            } catch (NumberFormatException e) {
                return fallbackScore;
            }
        }
        return fallbackScore;
    }

    private LeaderboardEntryDto toEntry(LeaderboardRow row) {
        return new LeaderboardEntryDto(
                row.playerId(),
                extractRawScore(row.metadata(), row.score()),
                row.rank(),
                filterInternalMetadata(row.metadata())
        );
    }
}
