package com.bitmask.leaderboard.dto.ranking.strategy;

import com.bitmask.leaderboard.domain.ranking.strategy.RankingStrategy;
import com.bitmask.leaderboard.domain.ranking.strategy.impl.DescendingRankingStrategy;

public record DescendingStrategyDto() implements RankingStrategyDto {
    @Override
    public RankingStrategy toDomain() {
        return new DescendingRankingStrategy();
    }
}
