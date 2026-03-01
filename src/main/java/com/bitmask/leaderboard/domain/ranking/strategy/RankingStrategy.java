package com.bitmask.leaderboard.domain.ranking.strategy;


public interface RankingStrategy {
    double score(double rawScore);

    boolean isAscending();
}
