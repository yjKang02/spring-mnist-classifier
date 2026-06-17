package com.nhnacademy.aimnistpractice.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = PixelValidator.class)
@Target({
        TYPE_USE,
        ANNOTATION_TYPE
})
@Retention(RUNTIME)
public @interface ValidPixel {
    String message() default "pixel must be a finite number between 0.0 and 1.0";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
