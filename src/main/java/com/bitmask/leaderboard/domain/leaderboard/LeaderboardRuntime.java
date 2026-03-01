package com.bitmask.leaderboard.domain.leaderboard;

import com.bitmask.leaderboard.domain.ranking.formula.RankingFormula;
import com.bitmask.leaderboard.domain.ranking.strategy.RankingStrategy;

import java.util.Map;

public record LeaderboardRuntime(
        RankingStrategy strategy,
        RankingFormula formula,
        Map<String, String> metadata
) {
}
