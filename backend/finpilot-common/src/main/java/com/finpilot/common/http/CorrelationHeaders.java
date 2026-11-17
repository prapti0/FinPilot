package com.finpilot.common.http;

public final class CorrelationHeaders {
    public static final String CORRELATION_ID = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    private CorrelationHeaders() {}
}