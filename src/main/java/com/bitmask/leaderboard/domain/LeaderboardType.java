package com.bitmask.leaderboard.domain;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;

public enum LeaderboardType {
    GLOBAL {
        @Override
        public String getSuffix(LocalDate now) {
            return "global";
        }
    },
    MONTHLY {
        @Override
        public String getSuffix(LocalDate now) {
            return now.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }
    },
    WEEKLY {
        @Override
        public String getSuffix(LocalDate now) {
            return now.get(IsoFields.WEEK_BASED_YEAR) + "-w" + now.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        }
    };

    public abstract String getSuffix(LocalDate now);

    public static LeaderboardType fromString(String value) {
        if (value == null) {
            return GLOBAL;
        }
        try {
            return LeaderboardType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid leaderboard type: " + value
            );
        }
    }
}
