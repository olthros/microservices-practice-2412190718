package com.zjgsu.czh.userservice.controller;

import com.zjgsu.czh.userservice.dto.LoginRequest;
import com.zjgsu.czh.userservice.dto.LoginResponse;
import com.zjgsu.czh.userservice.dto.MeResponse;
import com.zjgsu.czh.userservice.model.UserAccount;
import com.zjgsu.czh.userservice.repository.UserAccountRepository;
import com.zjgsu.czh.userservice.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        if (request == null
                || request.username() == null || request.username().isBlank()
                || request.password() == null || request.password().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username and password are required");
        }

        UserAccount account = userAccountRepository
                .findByUsername(request.username().trim())
                .orElse(null);

        if (account == null
                || !account.isEnabled()
                || !passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        String token = jwtService.issue(account);

        return new LoginResponse(
                token,
                "Bearer",
                jwtService.expiresInSeconds(),
                account.getUsername(),
                account.getRole());
    }

    @GetMapping("/me")
    public MeResponse me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        UserAccount account = userAccountRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        return new MeResponse(account.getId(), account.getUsername(), account.getRole());
    }
}
