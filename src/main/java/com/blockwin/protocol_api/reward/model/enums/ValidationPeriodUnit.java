package com.blockwin.protocol_api.reward.model.enums;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Unit in which a platform owner expresses the validation period of a reward deposit.
 */
public enum ValidationPeriodUnit {
    MINUTES(ChronoUnit.MINUTES),
    HOURS(ChronoUnit.HOURS),
    DAYS(ChronoUnit.DAYS);

    private final ChronoUnit chronoUnit;

    ValidationPeriodUnit(ChronoUnit chronoUnit) {
        this.chronoUnit = chronoUnit;
    }

    /**
     * @throws ArithmeticException if {@code amount} of this unit overflows a {@link Duration}
     */
    public Duration toDuration(long amount) {
        return Duration.of(amount, chronoUnit);
    }
}
