package com.bitmask.leaderboard.repository;

import com.bitmask.leaderboard.domain.leaderboard.LeaderboardConfiguration;

import java.util.Optional;
import java.util.Set;

public interface LeaderboardConfigRepository {

    void save(LeaderboardConfiguration configuration);

    Optional<LeaderboardConfiguration> findById(
            String gameId);

    void deleteById(String gameId);

    Set<String> getAllGameIds();
}
