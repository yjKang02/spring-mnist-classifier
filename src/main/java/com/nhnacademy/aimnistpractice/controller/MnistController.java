package com.nhnacademy.aimnistpractice.controller;

import com.nhnacademy.aimnistpractice.dto.MnistPredictionRequest;
import com.nhnacademy.aimnistpractice.dto.MnistPredictionResponse;
import com.nhnacademy.aimnistpractice.service.MnistModelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mnist")
@RequiredArgsConstructor
public class MnistController {
    private final MnistModelService mnistModelService;

    @PostMapping("/predict")
    public MnistPredictionResponse predict(@Valid @RequestBody MnistPredictionRequest predictionRequest) {
        return mnistModelService.predict(predictionRequest);
    }
}
