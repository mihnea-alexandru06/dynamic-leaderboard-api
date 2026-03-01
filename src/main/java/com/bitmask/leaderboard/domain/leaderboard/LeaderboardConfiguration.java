package com.bitmask.leaderboard.domain.leaderboard;

import com.bitmask.leaderboard.dto.ranking.formula.FormulaDto;
import com.bitmask.leaderboard.dto.ranking.strategy.RankingStrategyDto;

import java.util.Map;

public record LeaderboardConfiguration(
        String gameId,
        RankingStrategyDto strategy,
        FormulaDto formula,
        Map<String, String> metadata
) {
    public Map<String, String> metadata() {
        return metadata == null ? Map.of() : metadata;
    }
}
