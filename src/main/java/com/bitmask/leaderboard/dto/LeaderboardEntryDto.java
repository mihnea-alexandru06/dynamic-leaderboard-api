package com.bitmask.leaderboard.dto;

public record LeaderboardEntryDto(
        String playerId,
        double score,
        long rank
) {
}
