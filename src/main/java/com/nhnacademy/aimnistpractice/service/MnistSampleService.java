package com.nhnacademy.aimnistpractice.service;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class MnistSampleService {

    private static final double BRUSH_RADIUS = 1.2;
    private static final double[][][] STROKES = {
            {
                    {14, 4, 9, 5, 6, 10, 5, 16, 7, 22, 12, 25, 18, 24, 22, 19, 23, 12, 20, 6, 14, 4}
            },
            {
                    {15, 5, 11, 9, 15, 7, 15, 24},
                    {10, 24, 20, 24}
            },
            {
                    {7, 8, 11, 5, 17, 5, 21, 8, 21, 12, 17, 16, 8, 24, 22, 24}
            },
            {
                    {7, 7, 12, 5, 19, 6, 21, 10, 18, 14, 13, 14},
                    {18, 14, 22, 18, 20, 23, 14, 25, 8, 22}
            },
            {
                    {20, 24, 20, 5, 7, 18, 23, 18}
            },
            {
                    {21, 6, 9, 6, 8, 13, 15, 13, 21, 16, 21, 22, 16, 25, 9, 23}
            },
            {
                    {20, 7, 15, 5, 9, 9, 6, 16, 7, 22, 13, 25, 20, 22, 21, 17, 17, 14, 10, 15}
            },
            {
                    {7, 6, 22, 6, 15, 24}
            },
            {
                    {14, 4, 8, 7, 8, 12, 14, 15, 20, 12, 20, 7, 14, 4},
                    {14, 15, 8, 18, 8, 23, 14, 26, 20, 23, 20, 18, 14, 15}
            },
            {
                    {19, 15, 13, 15, 8, 12, 8, 7, 14, 4, 20, 7, 21, 13, 19, 20, 14, 24, 8, 24}
            }
    };

    public Map<Integer, double[]> getSamples() {
        Map<Integer, double[]> samples = new LinkedHashMap<>();
        for (int digit = 0; digit < STROKES.length; digit++) {
            samples.put(digit, render(STROKES[digit]));
        }
        return samples;
    }

    private double[] render(double[][] strokes) {
        double[] pixels = new double[784];
        for (double[] stroke : strokes) {
            for (int index = 0; index < stroke.length - 2; index += 2) {
                drawSegment(pixels, stroke[index], stroke[index + 1], stroke[index + 2], stroke[index + 3]);
            }
        }
        return pixels;
    }

    private void drawSegment(double[] pixels, double x1, double y1, double x2, double y2) {
        for (int y = 0; y < 28; y++) {
            for (int x = 0; x < 28; x++) {
                double distance = distanceToSegment(x + 0.5, y + 0.5, x1, y1, x2, y2);
                if (distance <= BRUSH_RADIUS + 1.0) {
                    double intensity = Math.max(0.0, 1.0 - Math.max(0.0, distance - BRUSH_RADIUS));
                    int pixelIndex = y * 28 + x;
                    pixels[pixelIndex] = Math.max(pixels[pixelIndex], intensity);
                }
            }
        }
    }

    private double distanceToSegment(double px, double py, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double lengthSquared = dx * dx + dy * dy;
        if (lengthSquared == 0.0) {
            return Math.hypot(px - x1, py - y1);
        }

        double t = ((px - x1) * dx + (py - y1) * dy) / lengthSquared;
        double clamped = Math.clamp(t, 0.0, 1.0);
        double projectionX = x1 + clamped * dx;
        double projectionY = y1 + clamped * dy;
        return Math.hypot(px - projectionX, py - projectionY);
    }
}
