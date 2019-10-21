package com.abehrdigital.payloadprocessor.utils;

import java.sql.Timestamp;
import java.util.Calendar;

public class RequestRoutineNextTryTimeCalculator {
    private static final int MINUTE_IN_SECONDS = 60;

    private static final int MAXIMUM_FIRST_CYCLE_TRY_COUNT = 15;
    private static final int MAXIMUM_SECOND_CYCLE_TRY_COUNT = 44;
    private static final int MAXIMUM_THIRD_CYCLE_TRY_COUNT = 86;

    private static final double FIRST_CYCLE_MULTIPLIER = 0.5;
    private static final int SECOND_CYCLE_MULTIPLIER = 15;
    private static final int THIRD_CYCLE_MULTIPLIER = 60;

    public static Timestamp getNextTryDateTimeIntervalInSeconds(int tryCount) {
        CycleIndex index = getCycleIndexByTryCount(tryCount);
        if (!index.equals(CycleIndex.FAILED_CYCLE)) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.SECOND, ((int) index.getIntervalForNextTryDateInSeconds(tryCount)));
            return new Timestamp(calendar.getTimeInMillis());
        } else {
            return null;
        }
    }

    private static CycleIndex getCycleIndexByTryCount(int tryCount) {
        if (tryCount < MAXIMUM_FIRST_CYCLE_TRY_COUNT) {
            return CycleIndex.FIRST_CYCLE;
        } else if (tryCount < MAXIMUM_SECOND_CYCLE_TRY_COUNT) {
            return CycleIndex.SECOND_CYCLE;
        } else if (tryCount < MAXIMUM_THIRD_CYCLE_TRY_COUNT) {
            return CycleIndex.THIRD_CYCLE;
        } else {
            return CycleIndex.FAILED_CYCLE;
        }
    }

    private enum CycleIndex {
        FIRST_CYCLE() {
            public double getIntervalForNextTryDateInSeconds(int tryCount) {
                return MINUTE_IN_SECONDS * tryCount * FIRST_CYCLE_MULTIPLIER;
            }
        },
        SECOND_CYCLE() {
            public double getIntervalForNextTryDateInSeconds(int tryCount) {
                return MINUTE_IN_SECONDS * SECOND_CYCLE_MULTIPLIER;
            }
        },
        THIRD_CYCLE() {
            public double getIntervalForNextTryDateInSeconds(int tryCount) {
                return MINUTE_IN_SECONDS * THIRD_CYCLE_MULTIPLIER;
            }
        },
        FAILED_CYCLE() {
            public double getIntervalForNextTryDateInSeconds(int tryCount) {
                return -1;
            }
        };

        public abstract double getIntervalForNextTryDateInSeconds(int tryCount);
    }
}