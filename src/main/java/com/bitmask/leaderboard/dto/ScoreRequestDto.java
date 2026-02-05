package com.bitmask.leaderboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ScoreRequestDto(
        @NotBlank String gameId,
        @NotBlank String playerId,
        @Positive double score
) {

}
