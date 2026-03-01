package com.bitmask.leaderboard.dto.leaderboard;

import java.util.Map;

public record LeaderboardEntryDto(
        String playerId,
        double score,
        long rank,
        Map<String, String> metadata) {
}
