package com.finpilot.ai.repo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;

@Repository
public class KnowledgeSearchRepository {
    private final JdbcTemplate jdbcTemplate;

    public KnowledgeSearchRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> searchByQuestion(String question, int limit) {
        String keyword = extractKeyword(question);
        return jdbcTemplate.query(
                """
                        SELECT chunk_text FROM knowledge_chunks
                        WHERE chunk_text ILIKE ?
                        ORDER BY id DESC
                        LIMIT ?
                        """,
                (rs, rowNum) -> rs.getString("chunk_text"),
                "%" + keyword + "%",
                limit);
    }

    private String extractKeyword(String question) {
        String trimmed = question.trim();
        if (trimmed.isBlank()) {
            return "portfolio";
        }
        String[] parts = trimmed.split("\\s+");
        return parts[0].toLowerCase(Locale.ROOT);
    }
}