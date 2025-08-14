package com.weatherapp.controller;

import com.weatherapp.dto.WeatherForecastDto;
import com.weatherapp.service.WeatherService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WeatherController.class)
@ActiveProfiles("test")
@DisplayName("Weather Controller Tests")
class WeatherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WeatherService weatherService;

    @Autowired
    private ObjectMapper objectMapper;

    private List<WeatherForecastDto> mockForecasts;

    @BeforeEach
    void setUp() {
        mockForecasts = createMockForecasts("London");
    }

    @Test
    @DisplayName("Should return weather forecast for valid city")
    @WithMockUser
    void shouldReturnWeatherForecastForValidCity() throws Exception {
        // Given
        String city = "London";
        when(weatherService.getWeatherForecast(city)).thenReturn(mockForecasts);

        // When & Then
        mockMvc.perform(get("/api/v1/weather/forecast")
                .param("city", city)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.city").value(city))
                .andExpect(jsonPath("$.forecasts").isArray())
                .andExpect(jsonPath("$.forecasts").isNotEmpty())
                .andExpect(jsonPath("$.total_days").value(3))
                .andExpect(jsonPath("$.offline_mode").value(false));

        verify(weatherService).getWeatherForecast(city);
    }

    @Test
    @DisplayName("Should return 400 for missing city parameter")
    @WithMockUser
    void shouldReturn400ForMissingCityParameter() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/weather/forecast")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(weatherService, never()).getWeatherForecast(any());
    }

    @Test
    @DisplayName("Should return 400 for empty city parameter")
    @WithMockUser
    void shouldReturn400ForEmptyCityParameter() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/weather/forecast")
                .param("city", "")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(weatherService, never()).getWeatherForecast(any());
    }

    @Test
    @DisplayName("Should return 500 when service throws exception")
    @WithMockUser
    void shouldReturn500WhenServiceThrowsException() throws Exception {
        // Given
        String city = "London";
        when(weatherService.getWeatherForecast(city))
                .thenThrow(new RuntimeException("Service error"));

        // When & Then
        mockMvc.perform(get("/api/v1/weather/forecast")
                .param("city", city)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(weatherService).getWeatherForecast(city);
    }

    @Test
    @DisplayName("Should return offline weather data")
    @WithMockUser
    void shouldReturnOfflineWeatherData() throws Exception {
        // Given
        String city = "London";
        when(weatherService.getOfflineWeatherData(city)).thenReturn(mockForecasts);

        // When & Then
        mockMvc.perform(get("/api/v1/weather/offline/{city}", city)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.city").value(city))
                .andExpect(jsonPath("$.forecasts").isArray())
                .andExpect(jsonPath("$.offline_mode").value(true));

        verify(weatherService).getOfflineWeatherData(city);
    }

    @Test
    @DisplayName("Should return health status when service fails")
    @WithMockUser
    void shouldReturnHealthStatus() throws Exception {
        // Given - Service throws exception (simulating failure)
        when(weatherService.getServiceHealth()).thenThrow(new RuntimeException("Service unavailable"));

        // When & Then
        mockMvc.perform(get("/api/v1/weather/health")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.service").value("weather-service"));

        verify(weatherService).getServiceHealth();
    }

    @Test
    @DisplayName("Should handle special characters in city name")
    @WithMockUser
    void shouldHandleSpecialCharactersInCityName() throws Exception {
        // Given
        String city = "São Paulo";
        List<WeatherForecastDto> saoPauloForecasts = createMockForecasts(city);
        when(weatherService.getWeatherForecast(city)).thenReturn(saoPauloForecasts);

        // When & Then
        mockMvc.perform(get("/api/v1/weather/forecast")
                .param("city", city)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value(city));

        verify(weatherService).getWeatherForecast(city);
    }

    @Test
    @DisplayName("Should handle long city names")
    @WithMockUser
    void shouldHandleLongCityNames() throws Exception {
        // Given
        String longCityName = "A".repeat(100);
        when(weatherService.getWeatherForecast(longCityName)).thenReturn(mockForecasts);

        // When & Then
        mockMvc.perform(get("/api/v1/weather/forecast")
                .param("city", longCityName)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(weatherService).getWeatherForecast(longCityName);
    }

    @Test
    @DisplayName("Should return CORS headers")
    @WithMockUser
    void shouldReturnCorsHeaders() throws Exception {
        // Given
        String city = "London";
        when(weatherService.getWeatherForecast(city)).thenReturn(mockForecasts);

        // When & Then
        mockMvc.perform(get("/api/v1/weather/forecast")
                .param("city", city)
                .header("Origin", "http://localhost:3000")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }

    // Helper methods
    private List<WeatherForecastDto> createMockForecasts(String city) {
        return List.of(
                createWeatherForecastDto(city, LocalDate.now(), 25.0, 15.0, "Sunny"),
                createWeatherForecastDto(city, LocalDate.now().plusDays(1), 23.0, 13.0, "Cloudy"),
                createWeatherForecastDto(city, LocalDate.now().plusDays(2), 20.0, 10.0, "Rainy")
        );
    }

    private WeatherForecastDto createWeatherForecastDto(String city, LocalDate date,
                                                       Double highTemp, Double lowTemp, String description) {
        WeatherForecastDto dto = new WeatherForecastDto();
        dto.setCity(city);
        dto.setForecastDate(date);
        dto.setHighTemp(highTemp);
        dto.setLowTemp(lowTemp);
        dto.setDescription(description);
        dto.setWeatherCondition("Clear");
        dto.setWindSpeed(5.0);
        dto.setHumidity(65);
        dto.setPressure(1013.25);
        dto.setSpecialCondition("Have a great day!");
        return dto;
    }
} 