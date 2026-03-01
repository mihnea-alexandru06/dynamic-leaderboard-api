package com.bitmask.leaderboard.dto.leaderboard;

public record PaginationDto(
        int offset,
        int limit,
        int count,
        int total
) {
}
