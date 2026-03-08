package com.victor.demo.validator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = StrongPasswordValidator.class)
public @interface StrongPassword {
    String message() default "Password must be strong";

    /**
     * One could use groups to categorize constrains, like Create vs Update.
     */
    Class<?>[] groups() default {};

    /**
     * One could use metadata payload to attach additional information to a validation failure.
     * This can be used to associate severity levels and error codes.
     * Ex: weak password can be a warning, while missing password can be an error.
     */
    Class<? extends Payload>[] payload() default {};
}
