package com.nhnacademy.aimnistpractice.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PixelsValidator implements ConstraintValidator<ValidPixels, double[]> {

    @Override
    public boolean isValid(double[] pixels, ConstraintValidatorContext context) {
        if (pixels == null) {
            return true;
        }

        for (double pixel : pixels) {
            if (Double.isNaN(pixel) || pixel < 0.0 || pixel > 1.0) {
                return false;
            }
        }
        return true;
    }
}
