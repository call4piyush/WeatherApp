package com.weatherapp.controller;

import com.weatherapp.dto.CityDto;
import com.weatherapp.service.CitySearchService;
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

import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cities")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "City Search", description = "City search and autocomplete endpoints with resilience")
public class CityController {
    
    private static final Logger logger = LoggerFactory.getLogger(CityController.class);
    
    @Autowired
    private CitySearchService citySearchService;
    
    @Operation(summary = "Search cities with autocomplete", 
               description = "Returns a list of cities matching the search query with automatic fallback to popular cities when external API is unavailable")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Successfully retrieved city suggestions",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = CityDto.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Invalid search query"),
        @ApiResponse(responseCode = "503", 
                    description = "City search service temporarily unavailable"),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error")
    })
    @GetMapping("/search")
    public ResponseEntity<?> searchCities(
            @Parameter(description = "City search query (minimum 2 characters)", example = "Lond")
            @RequestParam(required = false) @Size(min = 0, max = 100) String q) {
        
        try {
            logger.info("Received city search request for query: {}", q);
            
            List<CityDto> cities = citySearchService.searchCities(q);
            
            Map<String, Object> response = new HashMap<>();
            response.put("query", q);
            response.put("cities", cities);
            response.put("count", cities.size());
            response.put("timestamp", LocalDateTime.now());
            
            // Add metadata about the response
            boolean isFromFallback = cities.stream()
                .anyMatch(city -> citySearchService.getPopularCities().contains(city));
            
            if (q == null || q.trim().length() < 2) {
                response.put("type", "popular_cities");
                response.put("message", "Showing popular cities. Enter at least 2 characters to search.");
            } else if (isFromFallback && cities.size() <= 8) {
                response.put("type", "fallback_search");
                response.put("message", "Search service temporarily unavailable. Showing matching popular cities.");
            } else {
                response.put("type", "live_search");
                response.put("message", "Live search results from geocoding service.");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid search query: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Invalid search query: " + e.getMessage(), 400));
        } catch (Exception e) {
            logger.error("Error searching cities for query {}: {}", q, e.getMessage());
            
            // Determine appropriate HTTP status based on error type
            HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
            String message = "Unable to search cities. Please try again later.";
            
            if (e.getMessage().contains("unavailable") || e.getMessage().contains("timeout")) {
                status = HttpStatus.SERVICE_UNAVAILABLE;
                message = "City search service is temporarily unavailable. Please try again in a few minutes.";
            }
            
            return ResponseEntity.status(status)
                    .body(createErrorResponse(message, status.value()));
        }
    }
    
    @Operation(summary = "Get popular cities", 
               description = "Returns a list of popular cities for quick selection")
    @GetMapping("/popular")
    public ResponseEntity<?> getPopularCities() {
        try {
            logger.info("Received request for popular cities");
            
            List<CityDto> cities = citySearchService.getPopularCities();
            
            Map<String, Object> response = new HashMap<>();
            response.put("cities", cities);
            response.put("count", cities.size());
            response.put("type", "popular_cities");
            response.put("message", "Most popular cities worldwide");
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting popular cities: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Unable to fetch popular cities", 500));
        }
    }
    
    @Operation(summary = "City search health check", 
               description = "Check if city search service is operational")
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            // Test search with a simple query
            List<CityDto> testResult = citySearchService.searchCities("test");
            
            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("service", "city-search-service");
            health.put("test_search", testResult.size() > 0 ? "WORKING" : "LIMITED");
            health.put("fallback_enabled", true);
            health.put("popular_cities_count", citySearchService.getPopularCities().size());
            health.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            logger.error("City search health check failed: {}", e.getMessage());
            Map<String, Object> errorHealth = new HashMap<>();
            errorHealth.put("status", "DEGRADED");
            errorHealth.put("service", "city-search-service");
            errorHealth.put("error", e.getMessage());
            errorHealth.put("fallback_available", true);
            errorHealth.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorHealth);
        }
    }
    
    private Map<String, Object> createErrorResponse(String message, int statusCode) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("message", message);
        error.put("statusCode", statusCode);
        error.put("timestamp", LocalDateTime.now());
        error.put("service", "city-search-service");
        return error;
    }
} 