package com.victor.demo.service;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Geocodes free-text addresses into lat/lng coordinates using
 * OpenRouteService (https://openrouteservice.org).
 *
 * Caches results in-memory so we don't hit the API every refresh.
 */
@Service
public class GeocodingService {

    private static final String BASE_URL = "https://api.openrouteservice.org";

    private final WebClient webClient;
    private final String apiKey;
    private final Map<String, Coords> cache = new ConcurrentHashMap<>();

    public GeocodingService(@Value("${openrouteservice.api-key:}") String apiKey) {
        this.apiKey = apiKey;
        this.webClient = WebClient.builder().baseUrl(BASE_URL).build();
    }

    /**
     * Returns lat/lng for the given address. If the address is in cache, returns
     * instantly. Otherwise calls OpenRouteService and caches the result.
     * Returns Optional.empty() if geocoding fails.
     */
    public Optional<Coords> geocode(String address) {
        if (address == null || address.isBlank() || apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }

        Coords cached = cache.get(address);
        if (cached != null) return Optional.of(cached);

        try {
            Map<?, ?> response = webClient.get()
                    .uri(uri -> uri.path("/geocode/search")
                            .queryParam("api_key", apiKey)
                            .queryParam("text", address)
                            .queryParam("boundary.country", "RO")
                            .queryParam("size", 1)
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) return Optional.empty();

            List<?> features = (List<?>) response.get("features");
            if (features == null || features.isEmpty()) return Optional.empty();

            Map<?, ?> firstFeature = (Map<?, ?>) features.get(0);
            Map<?, ?> geometry = (Map<?, ?>) firstFeature.get("geometry");
            List<?> coordinates = (List<?>) geometry.get("coordinates");
            // OpenRouteService returns [lng, lat]
            double lng = ((Number) coordinates.get(0)).doubleValue();
            double lat = ((Number) coordinates.get(1)).doubleValue();

            Coords result = new Coords(lat, lng);
            cache.put(address, result);
            return Optional.of(result);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Data
    public static class Coords {
        private final double lat;
        private final double lng;
    }
}