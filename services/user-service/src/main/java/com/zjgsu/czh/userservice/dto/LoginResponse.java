package com.zjgsu.czh.userservice.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String username,
        String role) {
}
