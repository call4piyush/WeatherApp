package com.weatherapp.service;

import com.weatherapp.dto.OpenWeatherResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Service
public class ExternalWeatherApiService {
    
    private static final Logger logger = LoggerFactory.getLogger(ExternalWeatherApiService.class);
    private static final String RESILIENCE_NAME = "openweather-api";
    
    @Autowired
    private WebClient.Builder webClientBuilder;
    
    @Value("${openweather.api.key}")
    private String apiKey;
    
    @Value("${openweather.api.base-url}")
    private String baseUrl;
    
    @Value("${openweather.api.timeout:10000}")
    private int timeout;
    
    @Value("${openweather.api.connect-timeout:5000}")
    private int connectTimeout;
    
    @Value("${weather.forecast.days:3}")
    private int forecastDays;
    
    @CircuitBreaker(name = RESILIENCE_NAME, fallbackMethod = "fallbackWeatherData")
    @Retry(name = RESILIENCE_NAME)
    @RateLimiter(name = RESILIENCE_NAME)
    @TimeLimiter(name = RESILIENCE_NAME)
    public CompletableFuture<OpenWeatherResponse> getWeatherForecastAsync(String city) {
        logger.info("Fetching weather data from external API for city: {}", city);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = String.format("%s/forecast?q=%s&appid=%s&units=metric&cnt=%d", 
                        baseUrl, city, apiKey, forecastDays * 8);
                
                WebClient webClient = webClientBuilder
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                        .build();
                
                Mono<OpenWeatherResponse> responseMono = webClient.get()
                        .uri(url)
                        .retrieve()
                        .onStatus(
                            status -> status.is4xxClientError(),
                            response -> {
                                logger.error("Client error while calling weather API: {}", response.statusCode());
                                return Mono.error(new WeatherApiException("Invalid request to weather API", response.statusCode().value()));
                            })
                        .onStatus(
                            status -> status.is5xxServerError(),
                            response -> {
                                logger.error("Server error while calling weather API: {}", response.statusCode());
                                return Mono.error(new WeatherApiException("Weather API server error", response.statusCode().value()));
                            })
                        .bodyToMono(OpenWeatherResponse.class)
                        .timeout(Duration.ofMillis(timeout));
                
                OpenWeatherResponse response = responseMono.block();
                
                if (response == null || response.getForecastList() == null || response.getForecastList().isEmpty()) {
                    throw new WeatherApiException("Empty response from weather API", 204);
                }
                
                logger.info("Successfully fetched weather data for city: {}", city);
                return response;
                
            } catch (Exception e) {
                logger.error("Error fetching weather data for city {}: {}", city, e.getMessage());
                if (e instanceof WeatherApiException) {
                    throw e;
                }
                throw new WeatherApiException("Failed to fetch weather data: " + e.getMessage(), 500, e);
            }
        });
    }
    
    /**
     * Fallback method when the external API is unavailable
     */
    public CompletableFuture<OpenWeatherResponse> fallbackWeatherData(String city, Exception ex) {
        logger.warn("Fallback triggered for city {} due to: {}", city, ex.getMessage());
        
        return CompletableFuture.supplyAsync(() -> {
            // Return null to indicate API is unavailable
            // The calling service will handle this by returning cached data
            throw new WeatherApiUnavailableException(
                "Weather API is currently unavailable. Using cached data if available.", ex);
        });
    }
    
    /**
     * Synchronous method for backward compatibility
     */
    public OpenWeatherResponse getWeatherForecast(String city) {
        try {
            return getWeatherForecastAsync(city).get();
        } catch (Exception e) {
            if (e.getCause() instanceof WeatherApiUnavailableException) {
                throw (WeatherApiUnavailableException) e.getCause();
            }
            if (e.getCause() instanceof WeatherApiException) {
                throw (WeatherApiException) e.getCause();
            }
            throw new WeatherApiException("Failed to fetch weather data: " + e.getMessage(), 500, e);
        }
    }
    
    /**
     * Check if the external API is currently available
     */
    @CircuitBreaker(name = RESILIENCE_NAME)
    @Retry(name = RESILIENCE_NAME)
    public boolean isApiAvailable() {
        try {
            String healthUrl = baseUrl + "/weather?q=London&appid=" + apiKey;
            WebClient webClient = webClientBuilder.build();
            
            String response = webClient.get()
                    .uri(healthUrl)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(connectTimeout))
                    .block();
                    
            return response != null;
        } catch (Exception e) {
            logger.debug("Weather API health check failed: {}", e.getMessage());
            return false;
        }
    }
}

// Custom exceptions for better error handling
class WeatherApiException extends RuntimeException {
    private final int statusCode;
    
    public WeatherApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
    
    public WeatherApiException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
}

class WeatherApiUnavailableException extends RuntimeException {
    public WeatherApiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
} 