package com.nhnacademy.aimnistpractice.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PixelValidator implements ConstraintValidator<ValidPixel, Double> {

    @Override
    public boolean isValid(Double pixel, ConstraintValidatorContext context) {
        if (pixel == null) {
            return true;
        }

        return Double.isFinite(pixel)
                && pixel >= 0.0
                && pixel <= 1.0;
    }
}
