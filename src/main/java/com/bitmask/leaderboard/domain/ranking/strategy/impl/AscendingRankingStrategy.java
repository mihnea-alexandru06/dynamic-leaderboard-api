package com.bitmask.leaderboard.domain.ranking.strategy.impl;

import com.bitmask.leaderboard.domain.ranking.strategy.RankingStrategy;

public final class AscendingRankingStrategy implements RankingStrategy {
    @Override
    public double score(double rawScore) {
        return rawScore;
    }

    @Override
    public boolean isAscending() {
        return true;
    }
}
