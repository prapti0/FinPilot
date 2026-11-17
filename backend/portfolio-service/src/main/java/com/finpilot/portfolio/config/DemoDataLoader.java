package com.finpilot.portfolio.config;

import com.finpilot.portfolio.domain.PortfolioEntity;
import com.finpilot.portfolio.repo.PortfolioRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DemoDataLoader implements ApplicationRunner {
    private final PortfolioRepository portfolioRepository;
    private final JdbcTemplate jdbcTemplate;

    public DemoDataLoader(PortfolioRepository portfolioRepository, JdbcTemplate jdbcTemplate) {
        this.portfolioRepository = portfolioRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        Long userId = jdbcTemplate.query(
                        "SELECT id FROM users WHERE email = ?",
                        (rs, rowNum) -> rs.getLong("id"),
                        "demo@finpilot.com")
                .stream()
                .findFirst()
                .orElse(null);
        if (userId == null) {
            return;
        }
        if (portfolioRepository.findByUserIdAndName(userId, "Primary").isPresent()) {
            return;
        }
        PortfolioEntity portfolio = new PortfolioEntity();
        portfolio.setUserId(userId);
        portfolio.setName("Primary");
        portfolioRepository.save(portfolio);
    }
}