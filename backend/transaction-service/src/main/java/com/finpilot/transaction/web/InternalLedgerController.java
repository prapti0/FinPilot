package com.finpilot.transaction.web;

import com.finpilot.transaction.service.LedgerService;
import com.finpilot.transaction.service.ReserveFundsRequest;
import com.finpilot.transaction.web.dto.LedgerActionRequest;
import com.finpilot.transaction.web.dto.ReservationResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/ledger")
public class InternalLedgerController {
    private final LedgerService ledgerService;

    public InternalLedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @PostMapping("/reserve")
    public ReservationResponse reserve(@Valid @RequestBody ReserveFundsRequest request) {
        var result = ledgerService.reserveCashForBuy(request);
        return new ReservationResponse(result.accepted(), result.reason());
    }

    @PostMapping("/release")
    public void release(@Valid @RequestBody LedgerActionRequest request) {
        ledgerService.releaseReservation(
                request.userId(), request.orderId(), request.idempotencyKey(), request.amount());
    }

    @PostMapping("/settle")
    public void settle(@Valid @RequestBody LedgerActionRequest request) {
        ledgerService.settleBuy(request.userId(), request.orderId(), request.idempotencyKey(), request.amount());
    }
}