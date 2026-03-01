package com.bitmask.leaderboard.dto.leaderboard;

import com.bitmask.leaderboard.dto.ranking.formula.FormulaDto;
import com.bitmask.leaderboard.dto.ranking.strategy.RankingStrategyDto;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record CreateLeaderboardRequestDto(
        @NotBlank String gameId,
        @NotNull RankingStrategyDto rankingStrategy,
        @Nullable Double target,
        @NotNull FormulaDto formula,
        @Nullable Map<String, String> metadata
) {
}
