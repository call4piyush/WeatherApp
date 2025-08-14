package com.weatherapp.service;

import com.weatherapp.dto.CityDto;
import com.weatherapp.dto.GeocodingResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class CitySearchService {
    
    private static final Logger logger = LoggerFactory.getLogger(CitySearchService.class);
    private static final String RESILIENCE_NAME = "openweather-api";
    
    @Autowired
    private WebClient.Builder webClientBuilder;
    
    @Value("${openweather.api.key}")
    private String apiKey;
    
    @Value("${openweather.api.base-url}")
    private String baseUrl;
    
    @Value("${openweather.api.timeout:10000}")
    private int timeout;
    
    // Static fallback data for major cities
    private static final List<CityDto> POPULAR_CITIES = Arrays.asList(
        new CityDto("London", "United Kingdom", "GB", "England", 51.5074, -0.1278),
        new CityDto("New York", "United States", "US", "New York", 40.7128, -74.0060),
        new CityDto("Tokyo", "Japan", "JP", "Tokyo", 35.6762, 139.6503),
        new CityDto("Paris", "France", "FR", "Île-de-France", 48.8566, 2.3522),
        new CityDto("Berlin", "Germany", "DE", "Berlin", 52.5200, 13.4050),
        new CityDto("Sydney", "Australia", "AU", "New South Wales", -33.8688, 151.2093),
        new CityDto("Toronto", "Canada", "CA", "Ontario", 43.6532, -79.3832),
        new CityDto("Mumbai", "India", "IN", "Maharashtra", 19.0760, 72.8777),
        new CityDto("Beijing", "China", "CN", "Beijing", 39.9042, 116.4074),
        new CityDto("São Paulo", "Brazil", "BR", "São Paulo", -23.5505, -46.6333),
        new CityDto("Moscow", "Russia", "RU", "Moscow", 55.7558, 37.6176),
        new CityDto("Mexico City", "Mexico", "MX", "Mexico City", 19.4326, -99.1332),
        new CityDto("Cairo", "Egypt", "EG", "Cairo", 30.0444, 31.2357),
        new CityDto("Lagos", "Nigeria", "NG", "Lagos", 6.5244, 3.3792),
        new CityDto("Bangkok", "Thailand", "TH", "Bangkok", 13.7563, 100.5018)
    );
    
    @Cacheable(value = "citySearchCache", key = "#query")
    @CircuitBreaker(name = RESILIENCE_NAME, fallbackMethod = "fallbackCitySearch")
    @Retry(name = RESILIENCE_NAME)
    @RateLimiter(name = RESILIENCE_NAME)
    @TimeLimiter(name = RESILIENCE_NAME)
    public CompletableFuture<List<CityDto>> searchCitiesAsync(String query) {
        logger.info("Searching cities for query: {}", query);
        
        if (query == null || query.trim().length() < 2) {
            return CompletableFuture.completedFuture(getPopularCities(query));
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                String geocodingUrl = baseUrl.replace("/data/2.5", "/geo/1.0") + 
                                     "/direct?q=" + query + "&limit=10&appid=" + apiKey;
                
                WebClient webClient = webClientBuilder
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(512 * 1024))
                        .build();
                
                Mono<GeocodingResponse[]> responseMono = webClient.get()
                        .uri(geocodingUrl)
                        .retrieve()
                        .onStatus(
                            status -> status.is4xxClientError(),
                            response -> {
                                logger.error("Client error in city search API: {}", response.statusCode());
                                return Mono.error(new WeatherApiException("Invalid city search request", response.statusCode().value()));
                            })
                        .onStatus(
                            status -> status.is5xxServerError(),
                            response -> {
                                logger.error("Server error in city search API: {}", response.statusCode());
                                return Mono.error(new WeatherApiException("City search API server error", response.statusCode().value()));
                            })
                        .bodyToMono(GeocodingResponse[].class)
                        .timeout(Duration.ofMillis(timeout));
                
                GeocodingResponse[] responses = responseMono.block();
                
                if (responses == null || responses.length == 0) {
                    logger.warn("No cities found for query: {}", query);
                    return getPopularCities(query);
                }
                
                List<CityDto> cities = Arrays.stream(responses)
                        .map(this::convertToDto)
                        .filter(Objects::nonNull)
                        .distinct()
                        .limit(8)
                        .collect(Collectors.toList());
                
                logger.info("Found {} cities for query: {}", cities.size(), query);
                return cities;
                
            } catch (Exception e) {
                logger.error("Error searching cities for query {}: {}", query, e.getMessage());
                if (e instanceof WeatherApiException) {
                    throw e;
                }
                throw new WeatherApiException("Failed to search cities: " + e.getMessage(), 500, e);
            }
        });
    }
    
    /**
     * Fallback method when the external API is unavailable
     */
    public CompletableFuture<List<CityDto>> fallbackCitySearch(String query, Exception ex) {
        logger.warn("City search fallback triggered for query {} due to: {}", query, ex.getMessage());
        
        return CompletableFuture.supplyAsync(() -> {
            List<CityDto> fallbackCities = getPopularCities(query);
            logger.info("Returning {} fallback cities for query: {}", fallbackCities.size(), query);
            return fallbackCities;
        });
    }
    
    /**
     * Synchronous method for backward compatibility
     */
    public List<CityDto> searchCities(String query) {
        try {
            return searchCitiesAsync(query).get();
        } catch (Exception e) {
            logger.error("Error in synchronous city search for {}: {}", query, e.getMessage());
            return getPopularCities(query);
        }
    }
    
    private List<CityDto> getPopularCities(String query) {
        if (query == null || query.trim().isEmpty()) {
            return POPULAR_CITIES.subList(0, Math.min(8, POPULAR_CITIES.size()));
        }
        
        String lowerQuery = query.toLowerCase().trim();
        return POPULAR_CITIES.stream()
                .filter(city -> city.getName().toLowerCase().contains(lowerQuery) ||
                               city.getCountry().toLowerCase().contains(lowerQuery))
                .limit(8)
                .collect(Collectors.toList());
    }
    
    private CityDto convertToDto(GeocodingResponse response) {
        try {
            String countryName = getCountryName(response.getCountryCode());
            return new CityDto(
                response.getName(),
                countryName,
                response.getCountryCode(),
                response.getState(),
                response.getLatitude(),
                response.getLongitude()
            );
        } catch (Exception e) {
            logger.warn("Error converting geocoding response to DTO: {}", e.getMessage());
            return null;
        }
    }
    
    private String getCountryName(String countryCode) {
        // Basic country code to name mapping for common countries
        Map<String, String> countryNames = new HashMap<>();
        countryNames.put("US", "United States");
        countryNames.put("GB", "United Kingdom");
        countryNames.put("CA", "Canada");
        countryNames.put("AU", "Australia");
        countryNames.put("DE", "Germany");
        countryNames.put("FR", "France");
        countryNames.put("IT", "Italy");
        countryNames.put("ES", "Spain");
        countryNames.put("JP", "Japan");
        countryNames.put("CN", "China");
        countryNames.put("IN", "India");
        countryNames.put("BR", "Brazil");
        countryNames.put("RU", "Russia");
        countryNames.put("MX", "Mexico");
        
        return countryNames.getOrDefault(countryCode, countryCode);
    }
    
    /**
     * Get popular cities for quick access
     */
    public List<CityDto> getPopularCities() {
        return new ArrayList<>(POPULAR_CITIES.subList(0, Math.min(8, POPULAR_CITIES.size())));
    }
} 