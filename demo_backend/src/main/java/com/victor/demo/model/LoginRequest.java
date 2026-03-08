package com.victor.demo.model;

public record LoginRequest(
        String email,
        String password
) {
}
