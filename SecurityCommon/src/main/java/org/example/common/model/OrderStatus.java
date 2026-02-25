package org.example.common.model;

import java.util.EnumSet;

public enum OrderStatus {
    PENDING,
    STOCK_RESERVED,
    PAID,
    PAYMENT_FAILED,
    PAYMENT_ERROR,
    REFUNDED,
    REFUND_FAILED,
    REFUND_ERROR,
    CANCELLED,
    FAILED;

    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {
            case PENDING -> EnumSet.of(STOCK_RESERVED, FAILED, PAYMENT_FAILED, PAYMENT_ERROR, CANCELLED).contains(next);
            case STOCK_RESERVED -> EnumSet.of(PAID, PAYMENT_FAILED, PAYMENT_ERROR, CANCELLED).contains(next);
            case PAYMENT_FAILED,
                    PAYMENT_ERROR ->
                EnumSet.of(PAID, PAYMENT_FAILED, PAYMENT_ERROR, CANCELLED, FAILED).contains(next);
            case PAID -> EnumSet.of(REFUNDED, REFUND_FAILED, REFUND_ERROR, CANCELLED).contains(next);
            case REFUND_FAILED,
                    REFUND_ERROR ->
                EnumSet.of(REFUNDED, REFUND_FAILED, REFUND_ERROR).contains(next);
            case REFUNDED, CANCELLED, FAILED -> false;
        };
    }
}
