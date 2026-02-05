package com.bitmask.leaderboard.domain;

import java.time.LocalDate;

public record GameKey(
        String gameId,
        LeaderboardType type
) {
    public String toRedisKey(LocalDate now) {
        return "leaderboard:" + gameId + ":" + type.getSuffix(now);
    }
}
