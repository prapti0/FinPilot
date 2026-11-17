package com.finpilot.ai.web;

import com.finpilot.ai.service.AiAnalysisService;
import com.finpilot.ai.web.dto.*;
import com.finpilot.common.web.UserIdResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {
    private final AiAnalysisService aiAnalysisService;

    public AiController(AiAnalysisService aiAnalysisService) {
        this.aiAnalysisService = aiAnalysisService;
    }

    @PostMapping("/analyze-portfolio")
    public AnalysisResponse analyzePortfolio(
            HttpServletRequest request, @Valid @RequestBody AnalyzePortfolioRequest body) {
        return aiAnalysisService.analyzePortfolio(UserIdResolver.requireUserId(request), body);
    }

    @PostMapping("/explain-risk")
    public AnalysisResponse explainRisk(HttpServletRequest request, @Valid @RequestBody ExplainRiskRequest body) {
        return aiAnalysisService.explainRisk(UserIdResolver.requireUserId(request), body);
    }

    @PostMapping("/engineering/review")
    public EngineeringReviewResponse engineeringReview(
            HttpServletRequest request, @Valid @RequestBody EngineeringReviewRequest body) {
        return aiAnalysisService.engineeringReview(UserIdResolver.requireUserId(request), body);
    }
}