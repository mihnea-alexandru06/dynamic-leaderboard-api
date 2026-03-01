package com.bitmask.leaderboard.domain.leaderboard;

public record GameKey(
        String gameId,
        LeaderboardType type
) {
}
