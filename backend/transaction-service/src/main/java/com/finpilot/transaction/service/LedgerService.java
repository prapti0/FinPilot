package com.finpilot.transaction.service;

import com.finpilot.transaction.domain.AccountEntity;
import com.finpilot.transaction.domain.LedgerEntryEntity;
import com.finpilot.transaction.domain.OutboxEventEntity;
import com.finpilot.transaction.domain.TransactionEntity;
import com.finpilot.transaction.repo.AccountRepository;
import com.finpilot.transaction.repo.LedgerEntryRepository;
import com.finpilot.transaction.repo.OutboxEventRepository;
import com.finpilot.transaction.repo.TransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class LedgerService {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final OutboxEventRepository outboxEventRepository;

    public LedgerService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            LedgerEntryRepository ledgerEntryRepository,
            OutboxEventRepository outboxEventRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional
    public ReservationResult reserveCashForBuy(ReserveFundsRequest request) {
        return transactionRepository.findByIdempotencyKey(request.idempotencyKey())
                .map(tx -> ReservationResult.accepted(request.orderId(), request.idempotencyKey()))
                .orElseGet(() -> doReserve(request));
    }

    private ReservationResult doReserve(ReserveFundsRequest request) {
        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            return ReservationResult.rejected("INVALID_AMOUNT");
        }
        AccountEntity cash = accountRepository
                .findCashForUpdate(request.userId(), "CASH")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cash account missing"));
        AccountEntity clearing = accountRepository
                .findByUserIdAndAccountType(request.userId(), "CLEARING")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clearing account missing"));

        if (cash.getAvailableBalance().compareTo(request.amount()) < 0) {
            return ReservationResult.rejected("INSUFFICIENT_FUNDS");
        }

        cash.setAvailableBalance(cash.getAvailableBalance().subtract(request.amount()));
        cash.setReservedBalance(cash.getReservedBalance().add(request.amount()));

        TransactionEntity tx = new TransactionEntity();
        tx.setOrderId(request.orderId());
        tx.setIdempotencyKey(request.idempotencyKey());
        tx.setDescription("Reserve cash for order " + request.orderId());
        tx.setStatus("POSTED");
        tx = transactionRepository.save(tx);

        postBalancedPair(tx.getId(), clearing.getId(), cash.getId(), request.amount(), "RESERVE_CASH");

        OutboxEventEntity outbox = new OutboxEventEntity();
        outbox.setAggregateType("TRANSACTION");
        outbox.setAggregateId(String.valueOf(tx.getId()));
        outbox.setEventType("TransactionCompleted");
        outbox.setPayload(Map.of(
                "transactionId", tx.getId(),
                "orderId", request.orderId(),
                "idempotencyKey", request.idempotencyKey(),
                "status", "POSTED",
                "type", "RESERVE"));
        outboxEventRepository.save(outbox);

        return ReservationResult.accepted(request.orderId(), request.idempotencyKey());
    }

    @Transactional
    public void releaseReservation(Long userId, Long orderId, String idempotencyKey, BigDecimal amount) {
        if (transactionRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            return;
        }
        AccountEntity cash = accountRepository.findCashForUpdate(userId, "CASH")
                .orElseThrow();
        AccountEntity clearing = accountRepository.findByUserIdAndAccountType(userId, "CLEARING")
                .orElseThrow();

        BigDecimal release = amount.min(cash.getReservedBalance());
        cash.setReservedBalance(cash.getReservedBalance().subtract(release));
        cash.setAvailableBalance(cash.getAvailableBalance().add(release));

        TransactionEntity tx = new TransactionEntity();
        tx.setOrderId(orderId);
        tx.setIdempotencyKey(idempotencyKey);
        tx.setDescription("Release reservation for order " + orderId);
        tx.setStatus("POSTED");
        tx = transactionRepository.save(tx);

        postBalancedPair(tx.getId(), clearing.getId(), cash.getId(), release, "RELEASE_CASH");
    }

    @Transactional
    public void settleBuy(Long userId, Long orderId, String idempotencyKey, BigDecimal amount) {
        if (transactionRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            return;
        }
        AccountEntity cash = accountRepository.findCashForUpdate(userId, "CASH").orElseThrow();
        AccountEntity investment = accountRepository.findByUserIdAndAccountType(userId, "INVESTMENT")
                .orElseThrow();

        BigDecimal settle = amount.min(cash.getReservedBalance());
        cash.setReservedBalance(cash.getReservedBalance().subtract(settle));

        TransactionEntity tx = new TransactionEntity();
        tx.setOrderId(orderId);
        tx.setIdempotencyKey(idempotencyKey);
        tx.setDescription("Settle buy order " + orderId);
        tx.setStatus("POSTED");
        tx = transactionRepository.save(tx);

        LedgerEntryEntity debit = new LedgerEntryEntity();
        debit.setTransactionId(tx.getId());
        debit.setAccountId(investment.getId());
        debit.setDebit(settle);
        debit.setEntryType("BUY_INVESTMENT");
        LedgerEntryEntity credit = new LedgerEntryEntity();
        credit.setTransactionId(tx.getId());
        credit.setAccountId(cash.getId());
        credit.setCredit(settle);
        credit.setEntryType("SETTLE_RESERVED");
        ledgerEntryRepository.save(debit);
        ledgerEntryRepository.save(credit);
    }

    private void postBalancedPair(Long txId, Long debitAccountId, Long creditAccountId, BigDecimal amount, String type) {
        LedgerEntryEntity debit = new LedgerEntryEntity();
        debit.setTransactionId(txId);
        debit.setAccountId(debitAccountId);
        debit.setDebit(amount);
        debit.setEntryType(type + "_DR");
        LedgerEntryEntity credit = new LedgerEntryEntity();
        credit.setTransactionId(txId);
        credit.setAccountId(creditAccountId);
        credit.setCredit(amount);
        credit.setEntryType(type + "_CR");
        ledgerEntryRepository.save(debit);
        ledgerEntryRepository.save(credit);
    }

    public record ReservationResult(boolean accepted, Long orderId, String idempotencyKey, String reason) {
        public static ReservationResult accepted(Long orderId, String key) {
            return new ReservationResult(true, orderId, key, null);
        }

        public static ReservationResult rejected(String reason) {
            return new ReservationResult(false, null, null, reason);
        }
    }
}