package com.nhnacademy.aimnistpractice.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

// Constraint를 사용하여 Bean Validation 등록
@Documented
@Constraint(validatedBy = PixelsValidator.class)
@Target({
        ElementType.FIELD,
        ElementType.RECORD_COMPONENT,
        ElementType.PARAMETER
})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPixels {
    String message() default "pixels must be between 0.0 and 1.0";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
