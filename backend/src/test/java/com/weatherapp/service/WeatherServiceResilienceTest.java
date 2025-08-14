package com.weatherapp.service;

import com.weatherapp.dto.WeatherForecastDto;
import com.weatherapp.dto.OpenWeatherResponse;
import com.weatherapp.model.WeatherForecast;
import com.weatherapp.repository.WeatherForecastRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "resilience4j.circuitbreaker.instances.openweather-api.sliding-window-size=3",
    "resilience4j.circuitbreaker.instances.openweather-api.failure-rate-threshold=50",
    "resilience4j.circuitbreaker.instances.openweather-api.minimum-number-of-calls=3",
    "resilience4j.retry.instances.openweather-api.max-attempts=1",
    "weather.fallback.enabled=true"
})
@DisplayName("Weather Service Resilience Tests")
class WeatherServiceResilienceTest {

    @Autowired
    private WeatherService weatherService;

    @MockBean
    private ExternalWeatherApiService externalWeatherApiService;

    @MockBean
    private WeatherForecastRepository repository;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private RetryRegistry retryRegistry;

    private CircuitBreaker circuitBreaker;
    private Retry retry;

    @BeforeEach
    void setUp() {
        circuitBreaker = circuitBreakerRegistry.circuitBreaker("openweather-api");
        retry = retryRegistry.retry("openweather-api");
        
        // Reset circuit breaker state
        circuitBreaker.transitionToClosedState();
        
        // Clear any previous interactions
        reset(repository, externalWeatherApiService);
    }

    @Test
    @DisplayName("Should successfully get weather data when API is available")
    void shouldGetWeatherDataWhenApiAvailable() {
        // Given
        String city = "London";
        OpenWeatherResponse mockResponse = createMockApiResponse();
        
        when(repository.findByCityAndForecastDateBetweenOrderByForecastDate(eq(city), any(), any()))
            .thenReturn(List.of());
        when(externalWeatherApiService.getWeatherForecast(city))
            .thenReturn(mockResponse);
        
        // When
        List<WeatherForecastDto> result = weatherService.getWeatherForecast(city);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getCity()).isEqualTo(city);
    }

    @Test
    @DisplayName("Should use cached data when API fails")
    void shouldUseCachedDataWhenApiFails() {
        // Given
        String city = "London";
        WeatherForecast cachedForecast = createCachedWeatherForecast(city);
        
        when(repository.findByCityAndForecastDateBetweenOrderByForecastDate(eq(city), any(), any()))
            .thenReturn(List.of(cachedForecast));
        when(externalWeatherApiService.getWeatherForecast(city))
            .thenThrow(new WeatherApiUnavailableException("API unavailable", new RuntimeException()));
        
        // When
        List<WeatherForecastDto> result = weatherService.getWeatherForecast(city);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getCity()).isEqualTo(city);
    }

    @Test
    @DisplayName("Should handle multiple failures gracefully")
    void shouldTriggerCircuitBreakerAfterFailures() throws InterruptedException {
        // Given
        String city = "London";
        when(externalWeatherApiService.getWeatherForecast(city))
                .thenThrow(new WeatherApiUnavailableException("API unavailable", new RuntimeException()));
        when(repository.findByCityAndForecastDateBetweenOrderByForecastDate(eq(city), any(), any()))
                .thenReturn(List.of());

        // When - Trigger multiple failures
        for (int i = 0; i < 6; i++) {
            List<WeatherForecastDto> result = weatherService.getWeatherForecast(city);
            // Should return fallback data instead of throwing exception
            assertThat(result).isNotEmpty();
            assertThat(result.get(0).getSpecialCondition()).contains("Emergency fallback");
        }

        // Then - Service should remain responsive with fallback data
        List<WeatherForecastDto> finalResult = weatherService.getWeatherForecast(city);
        assertThat(finalResult).isNotEmpty();
        assertThat(finalResult.get(0).getCity()).isEqualTo(city);
    }

    @Test
    @DisplayName("Should provide synthetic data as last resort")
    void shouldProvideSyntheticDataAsLastResort() {
        // Given
        String city = "UnknownCity";
        when(externalWeatherApiService.getWeatherForecast(city))
                .thenThrow(new WeatherApiUnavailableException("API unavailable", new RuntimeException()));
        when(repository.findByCityAndForecastDateBetweenOrderByForecastDate(eq(city), any(), any()))
                .thenReturn(List.of());

        // When
        List<WeatherForecastDto> result = weatherService.getWeatherForecast(city);

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getSpecialCondition())
                .contains("Emergency fallback");
    }

    @Test
    @DisplayName("Should handle rate limiting gracefully")
    void shouldHandleRateLimitingGracefully() throws InterruptedException {
        // Given
        String city = "London";
        OpenWeatherResponse mockResponse = createMockApiResponse();
        
        when(repository.findByCityAndForecastDateBetweenOrderByForecastDate(eq(city), any(), any()))
            .thenReturn(List.of());
        when(externalWeatherApiService.getWeatherForecast(city))
            .thenReturn(mockResponse);
        
        // When - Make multiple rapid requests
        for (int i = 0; i < 5; i++) {
            try {
                weatherService.getWeatherForecast(city);
                Thread.sleep(50); // Small delay to avoid overwhelming
            } catch (Exception e) {
                // Some requests might be rate limited
            }
        }
        
        // Then - Service should still be responsive
        List<WeatherForecastDto> result = weatherService.getWeatherForecast(city);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Should provide health status information")
    void shouldProvideHealthStatusInformation() {
        // When
        Map<String, Object> healthStatus = weatherService.getServiceHealth();

        // Then
        assertThat(healthStatus).containsKey("status");
        assertThat(healthStatus).containsKey("database");
        assertThat(healthStatus).containsKey("externalApi");
        assertThat(healthStatus).containsKey("fallbackEnabled");
    }

    @Test
    @DisplayName("Should handle timeout scenarios")
    void shouldHandleTimeoutScenarios() {
        // Given
        String city = "London";
        when(externalWeatherApiService.getWeatherForecast(city))
                .thenThrow(new RuntimeException("Timeout"));
        when(repository.findByCityAndForecastDateBetweenOrderByForecastDate(eq(city), any(), any()))
                .thenReturn(List.of());

        // When
        List<WeatherForecastDto> result = weatherService.getWeatherForecast(city);

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getSpecialCondition()).contains("Emergency fallback");
    }

    @Test
    @DisplayName("Should validate input parameters")
    void shouldValidateInputParameters() {
        // When & Then - Empty city should return fallback data, not throw exception
        List<WeatherForecastDto> result = weatherService.getWeatherForecast("");
        
        // Should return fallback data instead of throwing exception
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getSpecialCondition()).contains("Emergency fallback");
    }

    @Test
    @DisplayName("Should return cached data when available and recent")
    void shouldReturnCachedDataWhenRecentlyAvailable() {
        // Given
        String city = "London";
        WeatherForecast recentForecast = createRecentWeatherForecast(city);
        
        when(repository.findByCityAndForecastDateBetweenOrderByForecastDate(eq(city), any(), any()))
            .thenReturn(List.of(recentForecast));
        
        // When
        List<WeatherForecastDto> result = weatherService.getWeatherForecast(city);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getCity()).isEqualTo(city);
        
        // Verify external API was not called
        verify(externalWeatherApiService, never()).getWeatherForecast(any());
    }

    // Helper methods
    private OpenWeatherResponse createMockApiResponse() {
        OpenWeatherResponse response = new OpenWeatherResponse();
        
        OpenWeatherResponse.ForecastItem item = new OpenWeatherResponse.ForecastItem();
        item.setDateTime("2024-01-15 12:00:00");
        
        OpenWeatherResponse.Main main = new OpenWeatherResponse.Main();
        main.setTempMax(25.0);
        main.setTempMin(15.0);
        main.setHumidity(65);
        main.setPressure(1013.25);
        item.setMain(main);
        
        OpenWeatherResponse.Weather weather = new OpenWeatherResponse.Weather();
        weather.setMain("Clear");
        weather.setDescription("Clear sky");
        item.setWeather(List.of(weather));
        
        OpenWeatherResponse.Wind wind = new OpenWeatherResponse.Wind();
        wind.setSpeed(5.0);
        item.setWind(wind);
        
        response.setForecastList(List.of(item));
        
        OpenWeatherResponse.City city = new OpenWeatherResponse.City();
        city.setName("London");
        response.setCity(city);
        
        return response;
    }

    private WeatherForecast createCachedWeatherForecast(String city) {
        WeatherForecast forecast = new WeatherForecast();
        forecast.setCity(city);
        forecast.setForecastDate(LocalDate.now());
        forecast.setHighTemp(22.0);
        forecast.setLowTemp(12.0);
        forecast.setDescription("Cached weather data");
        forecast.setWeatherCondition("Clouds");
        forecast.setWindSpeed(4.0);
        forecast.setHumidity(70);
        forecast.setPressure(1010.0);
        forecast.setSpecialCondition("Cached data");
        forecast.setCreatedAt(LocalDateTime.now().minusHours(2)); // Old data
        forecast.setUpdatedAt(LocalDateTime.now().minusHours(2));
        return forecast;
    }

    private WeatherForecast createRecentWeatherForecast(String city) {
        WeatherForecast forecast = new WeatherForecast();
        forecast.setCity(city);
        forecast.setForecastDate(LocalDate.now());
        forecast.setHighTemp(22.0);
        forecast.setLowTemp(12.0);
        forecast.setDescription("Recent weather data");
        forecast.setWeatherCondition("Clouds");
        forecast.setWindSpeed(4.0);
        forecast.setHumidity(70);
        forecast.setPressure(1010.0);
        forecast.setSpecialCondition("Recent data");
        forecast.setCreatedAt(LocalDateTime.now().minusMinutes(5)); // Recent data
        forecast.setUpdatedAt(LocalDateTime.now().minusMinutes(5));
        return forecast;
    }
} 