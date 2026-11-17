package com.finpilot.ai.service;

import com.finpilot.ai.client.GeminiClient;
import com.finpilot.ai.client.PortfolioClient;
import com.finpilot.ai.client.RiskClient;
import com.finpilot.ai.domain.AiReportEntity;
import com.finpilot.ai.repo.AiReportRepository;
import com.finpilot.ai.repo.KnowledgeSearchRepository;
import com.finpilot.ai.web.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Service
public class AiAnalysisService {
    private final AiReportRepository aiReportRepository;
    private final KnowledgeSearchRepository knowledgeSearchRepository;
    private final PortfolioClient portfolioClient;
    private final RiskClient riskClient;
    private final GeminiClient geminiClient;

    public AiAnalysisService(
            AiReportRepository aiReportRepository,
            KnowledgeSearchRepository knowledgeSearchRepository,
            PortfolioClient portfolioClient,
            RiskClient riskClient,
            GeminiClient geminiClient) {
        this.aiReportRepository = aiReportRepository;
        this.knowledgeSearchRepository = knowledgeSearchRepository;
        this.portfolioClient = portfolioClient;
        this.riskClient = riskClient;
        this.geminiClient = geminiClient;
    }

    @Transactional
    public AnalysisResponse analyzePortfolio(Long userId, AnalyzePortfolioRequest request) {
        PortfolioClient.PortfolioDetail portfolio =
                portfolioClient.fetchPortfolio(request.portfolioId(), userId);
        RiskClient.PortfolioRisk risk = riskClient.fetchPortfolioRisk(request.portfolioId());
        List<String> chunks = knowledgeSearchRepository.searchByQuestion(request.question(), 5);
        String prompt = buildPortfolioPrompt(portfolio, risk, chunks, request.question());
        String narrative =
                geminiClient.generate(prompt).orElseGet(() -> mockPortfolioNarrative(portfolio, risk, chunks, request.question()));
        return saveReport(userId, "PORTFOLIO_ANALYSIS", prompt, narrative, Map.of(
                "portfolioId", request.portfolioId(),
                "question", request.question(),
                "knowledgeHits", chunks.size()));
    }

    @Transactional
    public AnalysisResponse explainRisk(Long userId, ExplainRiskRequest request) {
        RiskClient.PortfolioRisk risk = riskClient.fetchPortfolioRisk(request.portfolioId());
        List<String> chunks = knowledgeSearchRepository.searchByQuestion("risk concentration", 3);
        String prompt = "Explain portfolio risk for portfolio "
                + request.portfolioId()
                + " sectors="
                + risk.sectorWeights();
        String narrative = geminiClient
                .generate(prompt)
                .orElseGet(() -> mockRiskExplanation(risk, chunks));
        return saveReport(userId, "RISK_EXPLANATION", prompt, narrative, Map.of("portfolioId", request.portfolioId()));
    }

    @Transactional
    public EngineeringReviewResponse engineeringReview(Long userId, EngineeringReviewRequest request) {
        List<EngineeringReviewResponse.Finding> findings = new ArrayList<>();
        if (request.diff() != null && request.diff().contains("password")) {
            findings.add(new EngineeringReviewResponse.Finding(
                    "HIGH", "Possible secret in diff", "Avoid committing credentials in code changes."));
        }
        if (request.diff() != null && request.diff().length() > 5000) {
            findings.add(new EngineeringReviewResponse.Finding(
                    "MEDIUM", "Large change set", "Consider splitting the pull request for easier review."));
        }
        if (findings.isEmpty()) {
            findings.add(new EngineeringReviewResponse.Finding(
                    "INFO", "No critical issues detected", "Mock review completed for " + request.repository()));
        }
        String summary = "Engineering review for PR '"
                + request.prTitle()
                + "' in "
                + request.repository()
                + ": "
                + findings.size()
                + " finding(s). This is an advisory mock review (no merge actions).";
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("prTitle", request.prTitle());
        metadata.put("repository", request.repository());
        metadata.put("findingCount", findings.size());
        saveReport(userId, "ENGINEERING_REVIEW", request.prTitle() + request.repository(), summary, metadata);
        return new EngineeringReviewResponse(request.prTitle(), request.repository(), summary, findings);
    }

    private AnalysisResponse saveReport(
            Long userId, String reportType, String prompt, String response, Map<String, Object> metadata) {
        AiReportEntity report = new AiReportEntity();
        report.setUserId(userId);
        report.setReportType(reportType);
        report.setPromptHash(sha256(prompt));
        report.setResponse(response);
        report.setMetadata(metadata);
        aiReportRepository.save(report);
        return new AnalysisResponse(reportType, response, metadata);
    }

    private String buildPortfolioPrompt(
            PortfolioClient.PortfolioDetail portfolio,
            RiskClient.PortfolioRisk risk,
            List<String> chunks,
            String question) {
        return "Portfolio "
                + portfolio.name()
                + " holdings="
                + portfolio.summary().holdingCount()
                + " invested="
                + portfolio.summary().investedValue()
                + " riskWeights="
                + risk.sectorWeights()
                + " context="
                + String.join(" | ", chunks)
                + " question="
                + question;
    }

    private String mockPortfolioNarrative(
            PortfolioClient.PortfolioDetail portfolio,
            RiskClient.PortfolioRisk risk,
            List<String> chunks,
            String question) {
        return "Simulated FinPilot analysis for portfolio '"
                + portfolio.name()
                + "': invested "
                + portfolio.summary().investedValue()
                + " INR across "
                + portfolio.summary().holdingCount()
                + " holdings. Top sector weights: "
                + risk.sectorWeights()
                + ". Knowledge context snippets: "
                + chunks.size()
                + ". Regarding your question ("
                + question
                + "), maintain diversification and monitor concentration in single sectors.";
    }

    private String mockRiskExplanation(RiskClient.PortfolioRisk risk, List<String> chunks) {
        return "Risk overview: total marked value "
                + risk.totalValue()
                + " INR. Sector weights "
                + risk.sectorWeights()
                + ". Guidance from knowledge base ("
                + chunks.size()
                + " hits): rebalance when any sector exceeds 40% and keep cash buffer for volatility.";
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            return null;
        }
    }
}