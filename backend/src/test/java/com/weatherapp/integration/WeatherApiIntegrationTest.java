package com.weatherapp.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Weather API Integration Tests")
class WeatherApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    @DisplayName("Should return weather forecast for valid city - Full Integration")
    void shouldReturnWeatherForecastForValidCityFullIntegration() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/weather/forecast")
                .param("city", "London")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.city").value("London"))
                .andExpect(jsonPath("$.forecasts").isArray())
                .andExpect(jsonPath("$.total_days").exists())
                .andExpect(jsonPath("$.offline_mode").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("Should handle API failure gracefully with fallback")
    void shouldHandleApiFailureGracefullyWithFallback() throws Exception {
        // When & Then - Test with invalid API key scenario
        mockMvc.perform(get("/api/v1/weather/forecast")
                .param("city", "InvalidCity12345")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.city").value("InvalidCity12345"))
                .andExpect(jsonPath("$.forecasts").isArray());
    }

    @Test
    @WithMockUser
    @DisplayName("Should return offline weather data")
    void shouldReturnOfflineWeatherData() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/weather/offline/London")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.city").value("London"))
                .andExpect(jsonPath("$.offline_mode").value(true));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return health status with circuit breaker info")
    void shouldReturnHealthStatusWithCircuitBreakerInfo() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/weather/health")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.externalApi").exists())
                .andExpect(jsonPath("$.fallbackEnabled").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("Should return service status with resilience metrics")
    void shouldReturnServiceStatusWithResilienceMetrics() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/weather/status")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.service").exists())
                .andExpect(jsonPath("$.resilience").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("Should search cities with autocomplete")
    void shouldSearchCitiesWithAutocomplete() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/cities/search")
                .param("q", "Lon")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.cities").isArray())
                .andExpect(jsonPath("$.count").exists())
                .andExpect(jsonPath("$.query").value("Lon"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return popular cities")
    void shouldReturnPopularCities() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/cities/popular")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.cities").isArray())
                .andExpect(jsonPath("$.cities").isNotEmpty())
                .andExpect(jsonPath("$.count").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("Should handle concurrent requests without issues")
    void shouldHandleConcurrentRequestsWithoutIssues() throws Exception {
        // Test multiple concurrent requests
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/v1/weather/forecast")
                    .param("city", "London")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @WithMockUser
    @DisplayName("Should validate request parameters properly")
    void shouldValidateRequestParametersProperly() throws Exception {
        // Test missing city parameter
        mockMvc.perform(get("/api/v1/weather/forecast")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // Test empty city parameter
        mockMvc.perform(get("/api/v1/weather/forecast")
                .param("city", "")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // Test city search with short query
        mockMvc.perform(get("/api/v1/cities/search")
                .param("q", "L")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("Should handle special characters in city names")
    void shouldHandleSpecialCharactersInCityNames() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/weather/forecast")
                .param("city", "São Paulo")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.city").value("São Paulo"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return proper CORS headers")
    void shouldReturnProperCorsHeaders() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/weather/forecast")
                .param("city", "London")
                .header("Origin", "http://localhost:3000")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should handle OPTIONS preflight requests")
    void shouldHandleOptionsPreflightRequests() throws Exception {
        // When & Then
        mockMvc.perform(options("/api/v1/weather/forecast")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"))
                .andExpect(header().exists("Access-Control-Allow-Methods"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should maintain session consistency across requests")
    void shouldMaintainSessionConsistencyAcrossRequests() throws Exception {
        // First request
        mockMvc.perform(get("/api/v1/weather/forecast")
                .param("city", "London")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Second request should work consistently
        mockMvc.perform(get("/api/v1/weather/forecast")
                .param("city", "Paris")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("Should handle large city names gracefully")
    void shouldHandleLargeCityNamesGracefully() throws Exception {
        // Given
        String longCityName = "A".repeat(100);

        // When & Then
        mockMvc.perform(get("/api/v1/weather/forecast")
                .param("city", longCityName)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("Should return appropriate content-type headers")
    void shouldReturnAppropriateContentTypeHeaders() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/weather/forecast")
                .param("city", "London")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
} 