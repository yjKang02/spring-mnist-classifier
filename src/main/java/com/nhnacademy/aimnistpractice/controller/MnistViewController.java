package com.nhnacademy.aimnistpractice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.aimnistpractice.service.MnistSampleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class MnistViewController {
    private final MnistSampleService sampleService;
    private final ObjectMapper objectMapper;

    @GetMapping({"/", "/mnist"})
    public String index(Model model) throws JsonProcessingException {
        model.addAttribute("samplesJson", objectMapper.writeValueAsString(sampleService.getSamples()));
        return "mnist";
    }
}
