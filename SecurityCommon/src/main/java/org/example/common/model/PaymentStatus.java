package org.example.common.model;

import java.util.EnumSet;

public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    REFUNDED,
    REFUND_FAILED;

    public boolean canTransitionTo(PaymentStatus next) {
        return switch (this) {
            case PENDING      -> EnumSet.of(SUCCESS, FAILED).contains(next);
            case SUCCESS      -> EnumSet.of(REFUNDED, REFUND_FAILED).contains(next);
            case REFUND_FAILED -> EnumSet.of(REFUNDED, REFUND_FAILED).contains(next);
            case FAILED, REFUNDED -> false;
        };
    }
}
