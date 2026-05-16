package com.victor.demo.service;

import com.victor.demo.dto.RevenuePointDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ForecastService {

    private final WebClient webClient;
    private final AnalyticsService analyticsService;

    public ForecastService(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
        this.webClient = WebClient.builder()
                .baseUrl("http://localhost:5000")
                .build();
    }

    public Map<String, Object> getForecast(int days) {
        // Pass null dates — service defaults to last 12 months of daily data
        List<RevenuePointDTO> history = analyticsService.getRevenue("daily", null, null);

        if (history.size() < 3) {
            return Map.of("error", "Not enough historical data for forecasting.");
        }

        Map<String, Object> body = new HashMap<>();
        body.put("data", history);
        body.put("days", days);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .uri("/predict")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return response != null ? response
                    : Map.of("error", "Empty response from forecasting service.");
        } catch (Exception e) {
            return Map.of(
                    "error", "Forecasting service unavailable.",
                    "details", e.getMessage()
            );
        }
    }
}
