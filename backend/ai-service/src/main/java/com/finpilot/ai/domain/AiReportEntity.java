package com.finpilot.ai.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "ai_reports")
public class AiReportEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "report_type", nullable = false)
    private String reportType;

    @Column(name = "prompt_hash")
    private String promptHash;

    @Column(nullable = false, columnDefinition = "text")
    private String response;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public void setPromptHash(String promptHash) {
        this.promptHash = promptHash;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}