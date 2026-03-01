package com.bitmask.leaderboard.dto.leaderboard;

import com.bitmask.leaderboard.domain.leaderboard.LeaderboardType;

import java.util.List;
import java.util.Map;

public record LeaderboardResponseDto(
        String gameId,
        LeaderboardType type,
        Map<String, String> metadata,
        PaginationDto pagination,
        List<LeaderboardEntryDto> entries
) {
}
