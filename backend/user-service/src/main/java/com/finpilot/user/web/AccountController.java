package com.finpilot.user.web;

import com.finpilot.common.web.UserIdResolver;
import com.finpilot.user.domain.AccountEntity;
import com.finpilot.user.repo.AccountRepository;
import com.finpilot.user.web.dto.BalanceResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {
    private final AccountRepository accountRepository;

    public AccountController(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @GetMapping("/me/balance")
    public BalanceResponse balance(HttpServletRequest request) {
        Long userId = UserIdResolver.requireUserId(request);
        AccountEntity cash = accountRepository
                .findByUserIdAndAccountTypeAndCurrency(userId, "CASH", "INR")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cash account not found"));
        return new BalanceResponse(
                cash.getAvailableBalance(),
                cash.getReservedBalance(),
                cash.getAvailableBalance().add(cash.getReservedBalance()));
    }
}