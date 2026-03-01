package com.bitmask.leaderboard.dto.ranking.strategy;

import com.bitmask.leaderboard.domain.ranking.strategy.RankingStrategy;
import com.bitmask.leaderboard.domain.ranking.strategy.impl.CloseToXRankingStrategy;

public record CloseToXStrategyDto(Double target) implements RankingStrategyDto {
    @Override
    public RankingStrategy toDomain() {
        return new CloseToXRankingStrategy(target);
    }
}
