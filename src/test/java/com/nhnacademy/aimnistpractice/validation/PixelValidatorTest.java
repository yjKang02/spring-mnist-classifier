package com.nhnacademy.aimnistpractice.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PixelValidatorTest {

    private final PixelValidator validator = new PixelValidator();

    @ParameterizedTest
    @MethodSource("validPixels")
    void acceptsFiniteValuesWithinPixelRange(Double pixel) {
        assertTrue(validator.isValid(pixel, null));
    }

    @ParameterizedTest
    @MethodSource("invalidPixels")
    void rejectsNonFiniteOrOutOfRangeValues(Double pixel) {
        assertFalse(validator.isValid(pixel, null));
    }

    @Test
    void delegatesNullValidationToNotNullConstraint() {
        assertTrue(validator.isValid(null, null));
    }

    private static Stream<Double> validPixels() {
        return Stream.of(
                0.0,
                Math.nextUp(0.0),
                0.5,
                Math.nextDown(1.0),
                1.0
        );
    }

    private static Stream<Double> invalidPixels() {
        return Stream.of(
                Math.nextDown(0.0),
                Math.nextUp(1.0),
                Double.NaN,
                Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY
        );
    }
}
