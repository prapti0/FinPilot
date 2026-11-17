package com.finpilot.transaction.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;

class LedgerServiceTest {

    @Test
    void rejectsNonPositiveAmountInResultType() {
        var result = LedgerService.ReservationResult.rejected("INVALID_AMOUNT");
        assertFalse(result.accepted());
    }

    @Test
    void reserveRequestBuilds() {
        var req = new ReserveFundsRequest(1L, 10L, new BigDecimal("8000.00"), "key-2");
        assertFalse(req.amount().signum() <= 0);
    }
}