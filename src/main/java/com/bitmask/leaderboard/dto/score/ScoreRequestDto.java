package com.bitmask.leaderboard.dto.score;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

public record ScoreRequestDto(
        @NotBlank String playerId,
        @NotEmpty Map<String, Double> values,
        Map<String, String> metadata) {

}
