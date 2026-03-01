package com.bitmask.leaderboard.controller;

import com.bitmask.leaderboard.domain.leaderboard.GameKey;
import com.bitmask.leaderboard.domain.leaderboard.LeaderboardType;
import com.bitmask.leaderboard.domain.ranking.strategy.RankingStrategy;
import com.bitmask.leaderboard.dto.leaderboard.CreateLeaderboardRequestDto;
import com.bitmask.leaderboard.dto.leaderboard.LeaderboardResponseDto;
import com.bitmask.leaderboard.dto.leaderboard.PlayerLeaderboardDetailsDto;
import com.bitmask.leaderboard.dto.ranking.formula.FormulaDto;
import com.bitmask.leaderboard.dto.score.ScoreRequestDto;
import com.bitmask.leaderboard.security.annotations.Authenticated;
import com.bitmask.leaderboard.security.annotations.Public;
import com.bitmask.leaderboard.service.LeaderboardService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/leaderboards")
@RequiredArgsConstructor
public class LeaderboardController {
    private final LeaderboardService leaderboardService;

    @Authenticated
    @PostMapping("/{gameId}/scores")
    public void submitScore(
            @PathVariable @NotBlank String gameId,
            @Valid @RequestBody ScoreRequestDto request) {

        leaderboardService.updateScore(gameId, request);
    }

    @Authenticated
    @DeleteMapping("/{gameId}")
    public void deleteLeaderboard(
            @PathVariable @NotBlank String gameId
    ) {
        leaderboardService.deleteLeaderboard(gameId);
    }

    @Authenticated
    @PostMapping
    public void createLeaderboard(@RequestBody CreateLeaderboardRequestDto request) {
        FormulaDto dto = request.formula();
        RankingStrategy rankingStrategy = request.rankingStrategy().toDomain();

        log.info(
                "Creating leaderboard: gameId={}, strategy={}, formulaVars={}",
                request.gameId(),
                rankingStrategy.getClass().getSimpleName(),
                dto.variables());

        leaderboardService.createLeaderboard(request);

        log.info("Leaderboard created: gameId={}", request.gameId());
    }


    @Public
    @GetMapping("/{gameId}")
    public LeaderboardResponseDto getLeaderboardWindow(
            @PathVariable @NotBlank String gameId,
            @RequestParam(defaultValue = "GLOBAL") LeaderboardType type,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int limit) {
        if (limit > 100) {
            throw new IllegalArgumentException("limit must be <= 100");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset cannot be negative");
        }

        GameKey key = new GameKey(gameId, type);
        return leaderboardService.getTopPlayers(key, offset, limit);
    }

    @Public
    @GetMapping("/{gameId}/entries/{playerId}")
    public PlayerLeaderboardDetailsDto getPlayerEntry(
            @PathVariable @NotBlank String gameId,
            @PathVariable @NotBlank String playerId,
            @RequestParam(required = false, defaultValue = "GLOBAL") LeaderboardType type) {
        GameKey key = new GameKey(gameId, type);
        return leaderboardService.getPlayerEntry(key, playerId);
    }

    @Public
    @GetMapping("/player/{playerId}")
    public List<PlayerLeaderboardDetailsDto> getPlayerEntries(
            @PathVariable @NotBlank String playerId
    ) {
        return leaderboardService.getPlayerEntries(playerId);
    }
}
