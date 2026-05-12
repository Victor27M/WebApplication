package com.victor.demo.service;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GeocodingService {

    private static final String BASE_URL = "https://api.openrouteservice.org";

    private static final Map<String, double[]> CITIES = new HashMap<>();

    static {
        CITIES.put("Cluj-Napoca",    new double[]{46.7712, 23.6236});
        CITIES.put("Cluj",           new double[]{46.7712, 23.6236});
        CITIES.put("București",      new double[]{44.4268, 26.1025});
        CITIES.put("Bucharest",      new double[]{44.4268, 26.1025});
        CITIES.put("Timișoara",      new double[]{45.7489, 21.2087});
        CITIES.put("Timisoara",      new double[]{45.7489, 21.2087});
        CITIES.put("Iași",           new double[]{47.1585, 27.6014});
        CITIES.put("Iasi",           new double[]{47.1585, 27.6014});
        CITIES.put("Constanța",      new double[]{44.1598, 28.6348});
        CITIES.put("Constanta",      new double[]{44.1598, 28.6348});
        CITIES.put("Craiova",        new double[]{44.3302, 23.7949});
        CITIES.put("Brașov",         new double[]{45.6427, 25.5887});
        CITIES.put("Brasov",         new double[]{45.6427, 25.5887});
        CITIES.put("Galați",         new double[]{45.4353, 28.0080});
        CITIES.put("Galati",         new double[]{45.4353, 28.0080});
        CITIES.put("Ploiești",       new double[]{44.9365, 26.0228});
        CITIES.put("Ploiesti",       new double[]{44.9365, 26.0228});
        CITIES.put("Oradea",         new double[]{47.0722, 21.9217});
        CITIES.put("Brăila",         new double[]{45.2692, 27.9574});
        CITIES.put("Braila",         new double[]{45.2692, 27.9574});
        CITIES.put("Bacău",          new double[]{46.5670, 26.9146});
        CITIES.put("Bacau",          new double[]{46.5670, 26.9146});
        CITIES.put("Arad",           new double[]{46.1866, 21.3123});
        CITIES.put("Pitești",        new double[]{44.8565, 24.8692});
        CITIES.put("Pitesti",        new double[]{44.8565, 24.8692});
        CITIES.put("Sibiu",          new double[]{45.7983, 24.1256});
        CITIES.put("Târgu Mureș",    new double[]{46.5386, 24.5581});
        CITIES.put("Targu Mures",    new double[]{46.5386, 24.5581});
        CITIES.put("Baia Mare",      new double[]{47.6567, 23.5850});
        CITIES.put("Buzău",          new double[]{45.1522, 26.8229});
        CITIES.put("Buzau",          new double[]{45.1522, 26.8229});
        CITIES.put("Satu Mare",      new double[]{47.7921, 22.8857});
        CITIES.put("Suceava",        new double[]{47.6514, 26.2556});
        CITIES.put("Piatra Neamț",   new double[]{46.9272, 26.3728});
        CITIES.put("Piatra Neamt",   new double[]{46.9272, 26.3728});
        CITIES.put("Deva",           new double[]{45.8833, 22.9167});
        CITIES.put("Alba Iulia",     new double[]{46.0737, 23.5800});
        CITIES.put("Focșani",        new double[]{45.6969, 27.1864});
        CITIES.put("Focsani",        new double[]{45.6969, 27.1864});
        CITIES.put("Tulcea",         new double[]{45.1792, 28.8008});
        CITIES.put("Reșița",         new double[]{45.3011, 21.8892});
        CITIES.put("Resita",         new double[]{45.3011, 21.8892});
        CITIES.put("Alexandria",     new double[]{43.9767, 25.3322});
        CITIES.put("Bistrița",       new double[]{47.1333, 24.5000});
        CITIES.put("Bistrita",       new double[]{47.1333, 24.5000});
        CITIES.put("Zalău",          new double[]{47.1905, 23.0570});
        CITIES.put("Zalau",          new double[]{47.1905, 23.0570});
        CITIES.put("Miercurea Ciuc", new double[]{46.3560, 25.7996});
        CITIES.put("Drobeta",        new double[]{44.6369, 22.6573});
        CITIES.put("Râmnicu Vâlcea", new double[]{45.1032, 24.3693});
        CITIES.put("Ramnicu Valcea", new double[]{45.1032, 24.3693});
    }

    private final WebClient           webClient;
    private final String              apiKey;
    private final Map<String, Coords> cache = new ConcurrentHashMap<>();

    public GeocodingService(@Value("${openrouteservice.api-key:}") String apiKey) {
        this.apiKey    = apiKey;
        this.webClient = WebClient.builder().baseUrl(BASE_URL).build();
    }

    public Optional<Coords> geocode(String address) {
        if (address == null || address.isBlank()) return Optional.empty();

        Coords hit = cache.get(address);
        if (hit != null) return Optional.of(hit);

        for (Map.Entry<String, double[]> entry : CITIES.entrySet()) {
            if (address.contains(entry.getKey())) {
                Coords coords = new Coords(entry.getValue()[0], entry.getValue()[1]);
                cache.put(address, coords);
                return Optional.of(coords);
            }
        }

        if (apiKey != null && !apiKey.isBlank() && !apiKey.equals("YOUR_KEY_HERE")) {
            return callApi(address);
        }

        return Optional.empty();
    }

    private Optional<Coords> callApi(String address) {
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

            Map<?, ?> geometry    = (Map<?, ?>) ((Map<?, ?>) features.get(0)).get("geometry");
            List<?>   coordinates = (List<?>) geometry.get("coordinates");

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