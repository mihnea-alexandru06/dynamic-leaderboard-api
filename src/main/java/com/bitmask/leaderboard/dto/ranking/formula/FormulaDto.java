package com.bitmask.leaderboard.dto.ranking.formula;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record FormulaDto(
        @NotBlank String expression,
        @NotNull Set<String> variables
) {
}
