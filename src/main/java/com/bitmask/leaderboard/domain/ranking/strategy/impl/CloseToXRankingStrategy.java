package com.bitmask.leaderboard.domain.ranking.strategy.impl;

import com.bitmask.leaderboard.domain.ranking.strategy.RankingStrategy;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class CloseToXRankingStrategy implements RankingStrategy {
    private final double target;

    @Override
    public double score(double rawScore) {
        return Math.abs(rawScore - target);
    }

    @Override
    public boolean isAscending() {
        return true;
    }
}
