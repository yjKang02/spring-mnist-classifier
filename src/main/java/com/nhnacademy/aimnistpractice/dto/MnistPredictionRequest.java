package com.nhnacademy.aimnistpractice.dto;

import com.nhnacademy.aimnistpractice.validation.ValidPixels;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MnistPredictionRequest(
        @NotNull
        @Size(min = 784, max = 784)
        @ValidPixels
        double[] pixels
) {
}
