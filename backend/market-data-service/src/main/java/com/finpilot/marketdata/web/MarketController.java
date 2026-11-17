package com.finpilot.marketdata.web;

import com.finpilot.marketdata.service.QuoteService;
import com.finpilot.marketdata.web.dto.PriceHistoryItem;
import com.finpilot.marketdata.web.dto.QuoteResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/market")
public class MarketController {
    private final QuoteService quoteService;

    public MarketController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @GetMapping("/quotes/{symbol}")
    public QuoteResponse quote(@PathVariable String symbol) {
        return quoteService.latestQuote(symbol);
    }

    @GetMapping("/quotes/{symbol}/history")
    public List<PriceHistoryItem> history(@PathVariable String symbol) {
        return quoteService.history(symbol);
    }
}