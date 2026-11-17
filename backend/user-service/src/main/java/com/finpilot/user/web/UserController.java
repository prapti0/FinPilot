package com.finpilot.user.web;

import com.finpilot.common.web.UserIdResolver;
import com.finpilot.user.service.AuthService;
import com.finpilot.user.web.dto.UserProfileResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    public UserProfileResponse me(HttpServletRequest request) {
        return authService.profile(UserIdResolver.requireUserId(request));
    }
}