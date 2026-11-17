package com.finpilot.common.web;

import com.finpilot.common.security.UserHeaders;
import jakarta.servlet.http.HttpServletRequest;

public final class UserIdResolver {
    private UserIdResolver() {}

    public static Long requireUserId(HttpServletRequest request) {
        String header = request.getHeader(UserHeaders.USER_ID);
        if (header == null || header.isBlank()) {
            throw new IllegalArgumentException("Missing user context");
        }
        return Long.parseLong(header);
    }
}