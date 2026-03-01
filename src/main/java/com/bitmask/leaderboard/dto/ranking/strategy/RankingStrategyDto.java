package com.bitmask.leaderboard.dto.ranking.strategy;

import com.bitmask.leaderboard.domain.ranking.strategy.RankingStrategy;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "name")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AscendingStrategyDto.class, name = "ASCENDING"),
        @JsonSubTypes.Type(value = DescendingStrategyDto.class, name = "DESCENDING"),
        @JsonSubTypes.Type(value = CloseToXStrategyDto.class, name = "CLOSE_TO_X")
})
public sealed interface RankingStrategyDto
        permits CloseToXStrategyDto, AscendingStrategyDto,
        DescendingStrategyDto {
    RankingStrategy toDomain();
}
