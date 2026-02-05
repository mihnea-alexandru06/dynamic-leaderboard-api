package com.bitmask.leaderboard.controller;

import com.bitmask.leaderboard.domain.GameKey;
import com.bitmask.leaderboard.dto.LeaderboardEntryDto;
import com.bitmask.leaderboard.domain.LeaderboardType;
import com.bitmask.leaderboard.dto.ScoreRequestDto;
import com.bitmask.leaderboard.service.LeaderboardService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/leaderboards")
@AllArgsConstructor
public class LeaderboardController {
    private final LeaderboardService leaderboardService;

    @PostMapping("/scores")
    public void submitScore(@Valid @RequestBody ScoreRequestDto request) {
        leaderboardService.updateScore(request);
    }

    @GetMapping("/{gameId}/top")
    public List<LeaderboardEntryDto> getTop(
            @PathVariable @NotBlank String gameId,
            @RequestParam(defaultValue = "GLOBAL") LeaderboardType type,
            @RequestParam(defaultValue = "10") int limit
    ) {
        GameKey key = new GameKey(gameId, type);
        return leaderboardService.getTopPlayers(key, limit);
    }

    @GetMapping("/{gameId}/rank/{playerId}")
    public LeaderboardEntryDto getPlayerRank(
            @PathVariable @NotBlank String gameId,
            @PathVariable @NotBlank String playerId,
            @RequestParam(required = false, defaultValue = "GLOBAL") LeaderboardType type
    ) {
        GameKey key = new GameKey(gameId, type);
        return leaderboardService.getPlayerRank(key, playerId);
    }
}
