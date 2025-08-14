package com.weatherapp.controller;

import com.weatherapp.dto.WeatherForecastDto;
import com.weatherapp.service.WeatherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/weather")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Weather Forecast", description = "Resilient weather forecast API endpoints")
public class WeatherController {
    
    private static final Logger logger = LoggerFactory.getLogger(WeatherController.class);
    
    @Autowired
    private WeatherService weatherService;
    
    @Operation(summary = "Get weather forecast for a city", 
               description = "Returns 3-day weather forecast for the specified city with automatic fallback to cached data when external API is unavailable")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Successfully retrieved weather forecast",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = WeatherForecastDto.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Invalid city parameter"),
        @ApiResponse(responseCode = "404", 
                    description = "Weather data not found for the city"),
        @ApiResponse(responseCode = "503", 
                    description = "Weather service temporarily unavailable"),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error")
    })
    @GetMapping("/forecast")
    public ResponseEntity<?> getWeatherForecast(
            @Parameter(description = "City name", example = "London", required = true)
            @RequestParam @NotBlank String city,
            @Parameter(description = "Offline mode flag", example = "false")
            @RequestParam(defaultValue = "false") boolean offline) {
        
        try {
            logger.info("Received weather forecast request for city: {}, offline: {}", city, offline);
            
            List<WeatherForecastDto> forecasts;
            boolean isFromCache = false;
            
            if (offline) {
                forecasts = weatherService.getOfflineWeatherData(city);
                isFromCache = true;
            } else {
                forecasts = weatherService.getWeatherForecast(city);
                // Check if any forecast has fallback indicators
                isFromCache = forecasts.stream()
                    .anyMatch(f -> f.getSpecialCondition() != null && 
                             f.getSpecialCondition().contains("⚠️"));
            }
            
            if (forecasts.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("No weather data available for " + city, 404));
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("city", city);
            response.put("forecasts", forecasts);
            response.put("offline_mode", offline);
            response.put("from_cache", isFromCache);
            response.put("total_days", forecasts.size());
            response.put("timestamp", LocalDateTime.now());
            
            if (isFromCache && !offline) {
                response.put("notice", "External weather service unavailable. Showing cached data.");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid city parameter: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Invalid city parameter: " + e.getMessage(), 400));
        } catch (Exception e) {
            logger.error("Error getting weather forecast for {}: {}", city, e.getMessage());
            
            // Determine appropriate HTTP status based on error type
            HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
            String message = "Unable to fetch weather data. Please try again later.";
            
            if (e.getMessage().contains("unavailable") || e.getMessage().contains("timeout")) {
                status = HttpStatus.SERVICE_UNAVAILABLE;
                message = "Weather service is temporarily unavailable. Please try again in a few minutes.";
            } else if (e.getMessage().contains("not found") || e.getMessage().contains("Invalid request")) {
                status = HttpStatus.NOT_FOUND;
                message = "Weather data not found for the specified city. Please check the city name.";
            }
            
            return ResponseEntity.status(status)
                    .body(createErrorResponse(message, status.value()));
        }
    }
    
    @Operation(summary = "Get offline weather data", 
               description = "Returns cached weather data for the specified city")
    @GetMapping("/offline/{city}")
    public ResponseEntity<?> getOfflineWeatherData(
            @Parameter(description = "City name", example = "London", required = true)
            @PathVariable @NotBlank String city) {
        
        try {
            logger.info("Received offline weather request for city: {}", city);
            
            List<WeatherForecastDto> forecasts = weatherService.getOfflineWeatherData(city);
            
            if (forecasts.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("No cached weather data available for " + city, 404));
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("city", city);
            response.put("forecasts", forecasts);
            response.put("offline_mode", true);
            response.put("from_cache", true);
            response.put("total_days", forecasts.size());
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting offline weather data for {}: {}", city, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Unable to fetch offline weather data", 500));
        }
    }
    
    @Operation(summary = "Health check endpoint", 
               description = "Returns comprehensive service health status including external API availability")
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            Map<String, Object> health = weatherService.getServiceHealth();
            
            // Determine overall status
            boolean isHealthy = "UP".equals(health.get("status")) && 
                              "UP".equals(health.get("database"));
            
            String overallStatus = isHealthy ? "UP" : "DEGRADED";
            if (!"UP".equals(health.get("externalApi"))) {
                overallStatus = "DEGRADED";
                health.put("message", "External weather API unavailable - using fallback data");
            }
            
            health.put("status", overallStatus);
            health.put("service", "weather-service");
            health.put("timestamp", LocalDateTime.now());
            
            HttpStatus httpStatus = "UP".equals(overallStatus) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
            return ResponseEntity.status(httpStatus).body(health);
            
        } catch (Exception e) {
            logger.error("Health check failed: {}", e.getMessage());
            Map<String, Object> errorHealth = new HashMap<>();
            errorHealth.put("status", "DOWN");
            errorHealth.put("service", "weather-service");
            errorHealth.put("error", e.getMessage());
            errorHealth.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorHealth);
        }
    }
    
    @Operation(summary = "Get service status", 
               description = "Detailed service status including resilience patterns state")
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getServiceStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "weather-service");
        status.put("version", "1.0.0");
        status.put("timestamp", LocalDateTime.now());
        
        try {
            Map<String, Object> health = weatherService.getServiceHealth();
            status.putAll(health);
            
            // Add resilience information
            status.put("resilience", Map.of(
                "circuitBreaker", "Enabled",
                "retry", "Enabled (3 attempts with exponential backoff)",
                "rateLimiter", "Enabled (60 requests per minute)",
                "timeout", "10 seconds",
                "fallback", "Enabled"
            ));
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            status.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(status);
        }
    }
    
    private Map<String, Object> createErrorResponse(String message, int statusCode) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("message", message);
        error.put("statusCode", statusCode);
        error.put("timestamp", LocalDateTime.now());
        error.put("service", "weather-service");
        return error;
    }
} 