package com.victor.demo.model;

import com.victor.demo.validator.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PersonCreateDTO {
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message =
            "Name should be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Password is required")
    @StrongPassword(message =
            "Password must contain at least 8 characters, including uppercase, " +
                    "lowercase, digit, and special character")
    private String password;

    @NotNull(message = "Age is required")
    private Integer age;

    @NotBlank(message = "Email is required")
    private String email;

    private PersonRole role = PersonRole.CUSTOMER;
}
