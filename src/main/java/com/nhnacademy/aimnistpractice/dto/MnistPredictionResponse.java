package com.nhnacademy.aimnistpractice.dto;

import java.util.Map;

public record MnistPredictionResponse(
        Integer prediction,
        Map<Integer, Double> probabilities
) {
}
