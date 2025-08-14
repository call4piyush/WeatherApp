package com.weatherapp.controller;

import com.weatherapp.dto.CityDto;
import com.weatherapp.service.CitySearchService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CityController.class)
@ActiveProfiles("test")
@DisplayName("City Controller Tests")
class CityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CitySearchService citySearchService;

    @Autowired
    private ObjectMapper objectMapper;

    private List<CityDto> mockCities;

    @BeforeEach
    void setUp() {
        mockCities = createMockCities();
    }

    @Test
    @WithMockUser
    @DisplayName("Should return cities for valid search query")
    void shouldReturnCitiesForValidSearchQuery() throws Exception {
        // Given
        String query = "Lon";
        when(citySearchService.searchCities(query)).thenReturn(mockCities);

        // When & Then
        mockMvc.perform(get("/api/v1/cities/search")
                .param("q", query)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.cities").isArray())
                .andExpect(jsonPath("$.cities").isNotEmpty())
                .andExpect(jsonPath("$.count").value(mockCities.size()))
                .andExpect(jsonPath("$.query").value(query));

        verify(citySearchService).searchCities(query);
    }

    @Test
    @WithMockUser
    @DisplayName("Should return popular cities for missing query parameter")
    void shouldReturn400ForMissingQueryParameter() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/cities/search")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("popular_cities"))
                .andExpect(jsonPath("$.count").value(0));

        verify(citySearchService).searchCities(null);
    }

    @Test
    @WithMockUser
    @DisplayName("Should return popular cities for empty query parameter")
    void shouldReturn400ForEmptyQueryParameter() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/cities/search")
                .param("q", "")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("popular_cities"))
                .andExpect(jsonPath("$.count").value(0));

        verify(citySearchService).searchCities("");
    }

    @Test
    @WithMockUser
    @DisplayName("Should return popular cities for query parameter too short")
    void shouldReturnPopularCitiesForQueryParameterTooShort() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/cities/search")
                .param("q", "L")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("popular_cities"))
                .andExpect(jsonPath("$.count").value(0));

        verify(citySearchService).searchCities("L");
    }

    @Test
    @WithMockUser
    @DisplayName("Should return popular cities")
    void shouldReturnPopularCities() throws Exception {
        // Given
        when(citySearchService.getPopularCities()).thenReturn(mockCities);

        // When & Then
        mockMvc.perform(get("/api/v1/cities/popular")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.cities").isArray())
                .andExpect(jsonPath("$.cities").isNotEmpty())
                .andExpect(jsonPath("$.count").value(mockCities.size()));

        verify(citySearchService).getPopularCities();
    }

    @Test
    @WithMockUser
    @DisplayName("Should return empty list when no cities found")
    void shouldReturnEmptyListWhenNoCitiesFound() throws Exception {
        // Given
        String query = "XYZ";
        when(citySearchService.searchCities(query)).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/v1/cities/search")
                .param("q", query)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.cities").isArray())
                .andExpect(jsonPath("$.cities").isEmpty())
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.query").value(query));

        verify(citySearchService).searchCities(query);
    }

    @Test
    @WithMockUser
    @DisplayName("Should handle service exceptions gracefully")
    void shouldHandleServiceExceptionsGracefully() throws Exception {
        // Given
        String query = "London";
        when(citySearchService.searchCities(query))
                .thenThrow(new RuntimeException("Service error"));

        // When & Then
        mockMvc.perform(get("/api/v1/cities/search")
                .param("q", query)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(citySearchService).searchCities(query);
    }

    @Test
    @WithMockUser
    @DisplayName("Should handle special characters in search query")
    void shouldHandleSpecialCharactersInSearchQuery() throws Exception {
        // Given
        String query = "São";
        when(citySearchService.searchCities(query)).thenReturn(mockCities);

        // When & Then
        mockMvc.perform(get("/api/v1/cities/search")
                .param("q", query)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value(query));

        verify(citySearchService).searchCities(query);
    }

    @Test
    @WithMockUser
    @DisplayName("Should handle case insensitive search")
    void shouldHandleCaseInsensitiveSearch() throws Exception {
        // Given
        String query = "LONDON";
        when(citySearchService.searchCities(query)).thenReturn(mockCities);

        // When & Then
        mockMvc.perform(get("/api/v1/cities/search")
                .param("q", query)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value(query));

        verify(citySearchService).searchCities(query);
    }

    @Test
    @WithMockUser
    @DisplayName("Should limit search results")
    void shouldLimitSearchResults() throws Exception {
        // Given
        String query = "New";
        List<CityDto> manyCities = createManyCities(20);
        when(citySearchService.searchCities(query)).thenReturn(manyCities.subList(0, 10));

        // When & Then
        mockMvc.perform(get("/api/v1/cities/search")
                .param("q", query)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cities").isArray())
                .andExpect(jsonPath("$.count").value(10));

        verify(citySearchService).searchCities(query);
    }

    @Test
    @WithMockUser
    @DisplayName("Should return CORS headers for city search")
    void shouldReturnCorsHeadersForCitySearch() throws Exception {
        // Given
        String query = "London";
        when(citySearchService.searchCities(query)).thenReturn(mockCities);

        // When & Then
        mockMvc.perform(get("/api/v1/cities/search")
                .param("q", query)
                .header("Origin", "http://localhost:3000")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }

    // Helper methods
    private List<CityDto> createMockCities() {
        return List.of(
                createCityDto("London", "GB", "England", 51.5074, -0.1278),
                createCityDto("London", "CA", "Ontario", 42.9834, -81.2497),
                createCityDto("London", "US", "Kentucky", 37.1289, -84.0833)
        );
    }

    private List<CityDto> createManyCities(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> createCityDto("City" + i, "US", "State" + i, 40.0 + i, -74.0 + i))
                .toList();
    }

    private CityDto createCityDto(String name, String countryCode, String state, 
                                 Double latitude, Double longitude) {
        CityDto dto = new CityDto();
        dto.setName(name);
        dto.setCountryCode(countryCode);
        dto.setState(state);
        dto.setLatitude(latitude);
        dto.setLongitude(longitude);
        dto.setDisplayName(name + ", " + state + ", " + countryCode);
        return dto;
    }
} 