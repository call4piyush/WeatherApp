package com.weatherapp.service;

import com.weatherapp.dto.WeatherForecastDto;
import com.weatherapp.dto.OpenWeatherResponse;
import com.weatherapp.model.WeatherForecast;
import com.weatherapp.repository.WeatherForecastRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class WeatherService {
    
    private static final Logger logger = LoggerFactory.getLogger(WeatherService.class);
    
    @Autowired
    private WeatherForecastRepository repository;
    
    @Autowired
    private ExternalWeatherApiService externalApiService;
    
    @Value("${weather.forecast.days:3}")
    private int forecastDays;
    
    @Value("${weather.fallback.enabled:true}")
    private boolean fallbackEnabled;
    
    @Value("${weather.fallback.data-age-threshold:1440}")
    private int dataAgeThresholdMinutes; // 24 hours by default
    
    @Cacheable(value = "weatherCache", key = "#city")
    public List<WeatherForecastDto> getWeatherForecast(String city) {
        logger.info("Getting weather forecast for city: {}", city);
        
        // Try to get from database first for recent data
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(forecastDays);
        
        List<WeatherForecast> cachedForecasts = repository
                .findByCityAndForecastDateBetweenOrderByForecastDate(city, today, endDate);
        
        // Check if we have recent data (within cache TTL)
        if (!cachedForecasts.isEmpty() && isDataRecent(cachedForecasts.get(0))) {
            logger.info("Returning cached weather data for {}", city);
            return convertToDto(cachedForecasts);
        }
        
        // Try to fetch from external API
        try {
            OpenWeatherResponse apiResponse = externalApiService.getWeatherForecast(city);
            List<WeatherForecast> freshForecasts = processApiResponse(apiResponse, city);
            logger.info("Successfully fetched fresh weather data for {}", city);
            return convertToDto(freshForecasts);
            
        } catch (WeatherApiUnavailableException e) {
            logger.warn("External weather API unavailable for {}: {}", city, e.getMessage());
            return handleApiUnavailable(city, cachedForecasts);
            
        } catch (WeatherApiException e) {
            logger.error("Weather API error for {}: {}", city, e.getMessage());
            if (e.getStatusCode() >= 400 && e.getStatusCode() < 500) {
                // Client error - don't use fallback for bad requests
                throw new RuntimeException("Invalid request: " + e.getMessage());
            }
            return handleApiUnavailable(city, cachedForecasts);
            
        } catch (Exception e) {
            logger.error("Unexpected error fetching weather data for {}: {}", city, e.getMessage());
            return handleApiUnavailable(city, cachedForecasts);
        }
    }
    
    private boolean isDataRecent(WeatherForecast forecast) {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(30); // 30 minutes cache
        return forecast.getUpdatedAt().isAfter(threshold);
    }
    
    private List<WeatherForecastDto> handleApiUnavailable(String city, List<WeatherForecast> cachedForecasts) {
        if (!fallbackEnabled) {
            throw new RuntimeException("Weather service is temporarily unavailable and fallback is disabled");
        }
        
        // Use cached data if available and not too old
        if (!cachedForecasts.isEmpty()) {
            WeatherForecast oldestForecast = cachedForecasts.get(0);
            LocalDateTime threshold = LocalDateTime.now().minusMinutes(dataAgeThresholdMinutes);
            
            if (oldestForecast.getUpdatedAt().isAfter(threshold)) {
                logger.info("Using cached fallback data for {} (age: {} minutes)", 
                    city, java.time.Duration.between(oldestForecast.getUpdatedAt(), LocalDateTime.now()).toMinutes());
                
                // Mark data as fallback by adding a special condition
                List<WeatherForecastDto> dtos = convertToDto(cachedForecasts);
                dtos.forEach(dto -> dto.setSpecialCondition(
                    "⚠️ Offline data - " + (dto.getSpecialCondition() != null ? dto.getSpecialCondition() : "Weather service temporarily unavailable")
                ));
                return dtos;
            } else {
                logger.warn("Cached data for {} is too old (age: {} minutes), threshold: {} minutes", 
                    city, java.time.Duration.between(oldestForecast.getUpdatedAt(), LocalDateTime.now()).toMinutes(), dataAgeThresholdMinutes);
            }
        }
        
        // Generate synthetic/mock data as last resort
        if (isEmergencyFallbackEnabled()) {
            logger.warn("Generating emergency fallback data for {}", city);
            return generateEmergencyFallbackData(city);
        }
        
        throw new RuntimeException("Weather data not available for " + city + " and no suitable fallback data found");
    }
    
    private boolean isEmergencyFallbackEnabled() {
        // You can make this configurable
        return true;
    }
    
    private List<WeatherForecastDto> generateEmergencyFallbackData(String city) {
        List<WeatherForecastDto> fallbackData = new ArrayList<>();
        LocalDate today = LocalDate.now();
        
        for (int i = 0; i < forecastDays; i++) {
            WeatherForecastDto dto = new WeatherForecastDto();
            dto.setCity(city);
            dto.setForecastDate(today.plusDays(i));
            dto.setHighTemp(20.0 + (Math.random() * 10)); // Random temp between 20-30°C
            dto.setLowTemp(dto.getHighTemp() - 10); // 10°C difference
            dto.setDescription("Weather data temporarily unavailable");
            dto.setWeatherCondition("Unknown");
            dto.setWindSpeed(5.0);
            dto.setHumidity(60);
            dto.setPressure(1013.25);
            dto.setSpecialCondition("⚠️ Emergency fallback - Weather service unavailable. Check weather conditions manually.");
            
            fallbackData.add(dto);
        }
        
        return fallbackData;
    }
    
    private List<WeatherForecast> processApiResponse(OpenWeatherResponse response, String city) {
        Map<LocalDate, List<OpenWeatherResponse.ForecastItem>> dailyForecasts = response.getForecastList()
                .stream()
                .collect(Collectors.groupingBy(item -> 
                    LocalDate.parse(item.getDateTime().split(" ")[0])));
        
        List<WeatherForecast> forecasts = new ArrayList<>();
        
        for (Map.Entry<LocalDate, List<OpenWeatherResponse.ForecastItem>> entry : dailyForecasts.entrySet()) {
            LocalDate date = entry.getKey();
            List<OpenWeatherResponse.ForecastItem> dayForecasts = entry.getValue();
            
            // Calculate daily highs and lows
            double highTemp = dayForecasts.stream()
                    .mapToDouble(f -> f.getMain().getTempMax())
                    .max().orElse(0.0);
            
            double lowTemp = dayForecasts.stream()
                    .mapToDouble(f -> f.getMain().getTempMin())
                    .min().orElse(0.0);
            
            // Get predominant weather condition
            String weatherCondition = dayForecasts.get(0).getWeather().get(0).getMain();
            String description = dayForecasts.get(0).getWeather().get(0).getDescription();
            
            // Calculate average wind speed
            double avgWindSpeed = dayForecasts.stream()
                    .mapToDouble(f -> f.getWind().getSpeed())
                    .average().orElse(0.0);
            
            // Calculate average humidity
            int avgHumidity = (int) dayForecasts.stream()
                    .mapToInt(f -> f.getMain().getHumidity())
                    .average().orElse(0);
            
            // Calculate average pressure
            double avgPressure = dayForecasts.stream()
                    .mapToDouble(f -> f.getMain().getPressure())
                    .average().orElse(0.0);
            
            WeatherForecast forecast = new WeatherForecast(city, date, highTemp, lowTemp, description);
            forecast.setWeatherCondition(weatherCondition);
            forecast.setWindSpeed(avgWindSpeed);
            forecast.setHumidity(avgHumidity);
            forecast.setPressure(avgPressure);
            
            // Add special conditions
            forecast.setSpecialCondition(generateSpecialCondition(highTemp, weatherCondition, avgWindSpeed, dayForecasts));
            
            // Save or update in database
            Optional<WeatherForecast> existing = repository.findByCityAndForecastDate(city, date);
            if (existing.isPresent()) {
                WeatherForecast existingForecast = existing.get();
                updateForecast(existingForecast, forecast);
                forecasts.add(repository.save(existingForecast));
            } else {
                forecasts.add(repository.save(forecast));
            }
        }
        
        return forecasts;
    }
    
    private String generateSpecialCondition(double highTemp, String weatherCondition, double windSpeed, 
                                           List<OpenWeatherResponse.ForecastItem> dayForecasts) {
        List<String> conditions = new ArrayList<>();
        
        // Temperature warnings
        if (highTemp > 40) {
            conditions.add("Use sunscreen lotion");
        }
        
        // Wind warnings
        if (windSpeed > 10) {
            conditions.add("It's too windy, watch out!");
        }
        
        // Rain warnings
        boolean hasRain = dayForecasts.stream()
                .anyMatch(f -> f.getRain() != null && f.getRain().getThreeHour() > 0);
        if (hasRain || weatherCondition.equalsIgnoreCase("Rain")) {
            conditions.add("Carry umbrella");
        }
        
        // Thunderstorm warnings
        if (weatherCondition.equalsIgnoreCase("Thunderstorm")) {
            conditions.add("Don't step out! A Storm is brewing!");
        }
        
        return conditions.isEmpty() ? "Have a great day!" : String.join(", ", conditions);
    }
    
    private void updateForecast(WeatherForecast existing, WeatherForecast updated) {
        existing.setHighTemp(updated.getHighTemp());
        existing.setLowTemp(updated.getLowTemp());
        existing.setDescription(updated.getDescription());
        existing.setWeatherCondition(updated.getWeatherCondition());
        existing.setWindSpeed(updated.getWindSpeed());
        existing.setHumidity(updated.getHumidity());
        existing.setPressure(updated.getPressure());
        existing.setSpecialCondition(updated.getSpecialCondition());
        existing.setUpdatedAt(LocalDateTime.now());
    }
    
    private List<WeatherForecastDto> convertToDto(List<WeatherForecast> forecasts) {
        return forecasts.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    private WeatherForecastDto convertToDto(WeatherForecast forecast) {
        WeatherForecastDto dto = new WeatherForecastDto();
        dto.setCity(forecast.getCity());
        dto.setForecastDate(forecast.getForecastDate());
        dto.setHighTemp(forecast.getHighTemp());
        dto.setLowTemp(forecast.getLowTemp());
        dto.setDescription(forecast.getDescription());
        dto.setWeatherCondition(forecast.getWeatherCondition());
        dto.setWindSpeed(forecast.getWindSpeed());
        dto.setHumidity(forecast.getHumidity());
        dto.setPressure(forecast.getPressure());
        dto.setSpecialCondition(forecast.getSpecialCondition());
        return dto;
    }
    
    public List<WeatherForecastDto> getOfflineWeatherData(String city) {
        logger.info("Getting offline weather data for city: {}", city);
        List<WeatherForecast> forecasts = repository.findByCityIgnoreCase(city);
        
        if (forecasts.isEmpty()) {
            logger.warn("No offline data available for {}", city);
            if (isEmergencyFallbackEnabled()) {
                return generateEmergencyFallbackData(city);
            }
            throw new RuntimeException("No offline weather data available for " + city);
        }
        
        return convertToDto(forecasts);
    }
    
    /**
     * Get service health status including external API availability
     */
    public Map<String, Object> getServiceHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("database", "UP"); // Simplified - you could add actual DB health check
        
        try {
            boolean apiAvailable = externalApiService.isApiAvailable();
            health.put("externalApi", apiAvailable ? "UP" : "DOWN");
            health.put("fallbackEnabled", fallbackEnabled);
        } catch (Exception e) {
            health.put("externalApi", "DOWN");
            health.put("externalApiError", e.getMessage());
        }
        
        return health;
    }
} 