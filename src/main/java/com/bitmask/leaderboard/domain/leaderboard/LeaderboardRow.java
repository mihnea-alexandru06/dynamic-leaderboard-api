package com.bitmask.leaderboard.domain.leaderboard;

import java.util.Map;

public record LeaderboardRow(
        String playerId,
        double score,
        long rank,
        Map<String, String> metadata
) {
}
