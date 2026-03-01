package com.bitmask.leaderboard.mapping.converter;

import com.bitmask.leaderboard.domain.leaderboard.LeaderboardType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class LeaderboardTypeConverter
        implements Converter<String, LeaderboardType> {

    @Override
    public LeaderboardType convert(String source) {
        return LeaderboardType.fromString(source);
    }
}

