package com.finpilot.user.config;

import com.finpilot.user.domain.AccountEntity;
import com.finpilot.user.domain.RoleEntity;
import com.finpilot.user.domain.UserEntity;
import com.finpilot.user.repo.AccountRepository;
import com.finpilot.user.repo.RoleRepository;
import com.finpilot.user.repo.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DemoDataLoader implements ApplicationRunner {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataLoader(
            UserRepository userRepository,
            RoleRepository roleRepository,
            AccountRepository accountRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.findByEmail("demo@finpilot.com").isPresent()) {
            return;
        }
        RoleEntity role = roleRepository.findByName("USER").orElseThrow();
        UserEntity user = new UserEntity();
        user.setName("Demo Investor");
        user.setEmail("demo@finpilot.com");
        user.setPasswordHash(passwordEncoder.encode("demo1234"));
        user.setRole(role);
        user = userRepository.save(user);

        AccountEntity cash = new AccountEntity();
        cash.setUserId(user.getId());
        cash.setAccountType("CASH");
        cash.setAvailableBalance(new BigDecimal("50000.0000"));
        accountRepository.save(cash);
    }
}