package com.nhnacademy.aimnistpractice.dto;

import com.nhnacademy.aimnistpractice.validation.ValidPixel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record MnistPredictionRequest(
        @NotNull
        @Size(min = 784, max = 784)
        List<@NotNull @ValidPixel Double> pixels
) {
}
