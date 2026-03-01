package com.bitmask.leaderboard.repository;

import com.bitmask.leaderboard.domain.leaderboard.GameKey;
import com.bitmask.leaderboard.domain.leaderboard.LeaderboardRow;
import com.bitmask.leaderboard.domain.ranking.strategy.RankingStrategy;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LeaderboardDataRepository {

    void upsertScore(
            GameKey key,
            LocalDate date,
            String playerId,
            double score,
            Map<String, String> metadata);

    List<LeaderboardRow> getTopRange(
            GameKey key,
            RankingStrategy strategy,
            LocalDate date,
            int offset,
            int limit);

    Long getRank(
            GameKey key,
            RankingStrategy strategy,
            LocalDate date,
            String playerId);

    Optional<LeaderboardRow> getPlayer(
            GameKey key,
            RankingStrategy strategy,
            LocalDate date,
            String playerId);

    Long getTotalSize(
            GameKey key,
            LocalDate date);

    void deleteLeaderboard(String gameId);
}
