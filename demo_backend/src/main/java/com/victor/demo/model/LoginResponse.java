package com.victor.demo.model;

import java.util.UUID;

public record LoginResponse(
        Boolean success,
        String role,
        UUID personId,
        String token,
        String errorMessage
) {
}