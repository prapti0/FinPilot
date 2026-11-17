package com.finpilot.common.security;

public final class UserHeaders {
    private UserHeaders() {}

    public static final String USER_ID = JwtSupport.HEADER_USER_ID;
    public static final String USER_EMAIL = JwtSupport.HEADER_USER_EMAIL;
}