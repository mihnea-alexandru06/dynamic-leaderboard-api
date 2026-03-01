package com.bitmask.leaderboard.service;

import com.bitmask.leaderboard.domain.leaderboard.LeaderboardConfiguration;
import com.bitmask.leaderboard.domain.leaderboard.LeaderboardRuntime;
import com.bitmask.leaderboard.domain.ranking.formula.RankingFormula;
import com.bitmask.leaderboard.error.exception.NotFoundException;
import com.bitmask.leaderboard.repository.LeaderboardConfigRepository;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@AllArgsConstructor
@Service
public class LeaderboardRuntimeProvider {
    private final LeaderboardConfigRepository configRepository;

    private final Map<String, LeaderboardRuntime> cache = new ConcurrentHashMap<>();
    private final Set<String> cachedGameIds = ConcurrentHashMap.newKeySet();

    @PostConstruct
    public void init() {
        cachedGameIds.addAll(
                configRepository.getAllGameIds()
        );
        log.info("Game Ids have been cached");
    }

    public LeaderboardRuntime get(String key) {
        return cache.computeIfAbsent(key, this::build);
    }

    private LeaderboardRuntime build(String key) {
        LeaderboardConfiguration cfg = configRepository.findById(key)
                .orElseThrow(() -> new NotFoundException("Leaderboard '" + key + "' not found"));

        return new LeaderboardRuntime(
                cfg.strategy().toDomain(),
                new RankingFormula(cfg.formula().expression(), cfg.formula().variables()),
                cfg.metadata());
    }

    public void evict(String key) {
        cache.remove(key);
        cachedGameIds.remove(key);

    }

    public Set<String> getAllGameIds() {
        return cachedGameIds;
    }

    public void registerNewGame(String key) {
        cachedGameIds.add(key);
    }
}
