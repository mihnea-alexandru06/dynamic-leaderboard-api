package com.bitmask.leaderboard.dto.leaderboard;

import com.bitmask.leaderboard.domain.leaderboard.LeaderboardType;

import java.util.Map;

public record PlayerLeaderboardDetailsDto(
        String gameId,
        LeaderboardType type,
        Map<String, String> metadata,
        LeaderboardEntryDto entry
) {
}
