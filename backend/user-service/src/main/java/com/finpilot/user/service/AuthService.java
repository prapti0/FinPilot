package com.finpilot.user.service;

import com.finpilot.common.security.JwtSupport;
import com.finpilot.user.config.AppProperties;
import com.finpilot.user.domain.AccountEntity;
import com.finpilot.user.domain.RoleEntity;
import com.finpilot.user.domain.UserEntity;
import com.finpilot.user.repo.AccountRepository;
import com.finpilot.user.repo.RoleRepository;
import com.finpilot.user.repo.UserRepository;
import com.finpilot.user.web.dto.AuthResponse;
import com.finpilot.user.web.dto.LoginRequest;
import com.finpilot.user.web.dto.RegisterRequest;
import com.finpilot.user.web.dto.UserProfileResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            AccountRepository accountRepository,
            PasswordEncoder passwordEncoder,
            AppProperties appProperties) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.appProperties = appProperties;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        RoleEntity role = roleRepository.findByName("USER")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Role USER missing"));

        UserEntity user = new UserEntity();
        user.setName(request.name());
        user.setEmail(request.email().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(role);
        user = userRepository.save(user);

        AccountEntity cash = new AccountEntity();
        cash.setUserId(user.getId());
        cash.setAccountType("CASH");
        cash.setAvailableBalance(new BigDecimal("50000.0000"));
        accountRepository.save(cash);

        return tokenResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return tokenResponse(user);
    }

    public UserProfileResponse profile(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return new UserProfileResponse(user.getId(), user.getName(), user.getEmail(), user.getRole().getName());
    }

    private AuthResponse tokenResponse(UserEntity user) {
        Instant exp = Instant.now().plus(24, ChronoUnit.HOURS);
        String token = JwtSupport.createToken(
                appProperties.jwtSecret(),
                user.getId(),
                user.getEmail(),
                user.getRole().getName(),
                exp);
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getName(), user.getRole().getName());
    }
}