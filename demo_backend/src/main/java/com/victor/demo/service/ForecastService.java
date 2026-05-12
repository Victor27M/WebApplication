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

    /**
     * Fetches recent daily revenue, sends it to the Python microservice,
     * and returns the prediction payload. Falls back gracefully if Python
     * service is unavailable.
     */
    public Map<String, Object> getForecast(int days) {
        List<RevenuePointDTO> history = analyticsService.getRevenue("daily");

        if (history.size() < 3) {
            return Map.of("error", "Not enough historical data for forecasting.");
        }

        Map<String, Object> body = new HashMap<>();
        body.put("data", history);
        body.put("days", days);

        try {
            Map<?, ?> response = webClient.post()
                    .uri("/predict")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return response != null ? (Map<String, Object>) response
                    : Map.of("error", "Empty response from forecasting service.");
        } catch (Exception e) {
            return Map.of(
                    "error", "Forecasting service unavailable.",
                    "details", e.getMessage()
            );
        }
    }
}
