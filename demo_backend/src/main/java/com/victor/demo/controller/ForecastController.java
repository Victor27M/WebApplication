package com.victor.demo.controller;

import com.victor.demo.service.ForecastService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/analytics")
@AllArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class ForecastController {

    private final ForecastService forecastService;

    /**
     * GET /analytics/forecast?days=14
     * Calls the Python microservice with the last 90 days of daily revenue
     * and returns predicted values + confidence interval.
     */
    @GetMapping("/forecast")
    public ResponseEntity<Map<String, Object>> getForecast(
            @RequestParam(defaultValue = "14") int days) {
        return ResponseEntity.ok(forecastService.getForecast(days));
    }
}
