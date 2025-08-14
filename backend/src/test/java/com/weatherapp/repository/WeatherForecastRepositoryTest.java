package com.weatherapp.repository;

import com.weatherapp.model.WeatherForecast;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Weather Forecast Repository Tests")
class WeatherForecastRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private WeatherForecastRepository repository;

    private WeatherForecast londonForecast;
    private WeatherForecast parisforecast;

    @BeforeEach
    void setUp() {
        londonForecast = createWeatherForecast("London", LocalDate.now(), 25.0, 15.0);
        parisforecast = createWeatherForecast("Paris", LocalDate.now().plusDays(1), 22.0, 12.0);
        
        entityManager.persistAndFlush(londonForecast);
        entityManager.persistAndFlush(parisforecast);
    }

    @Test
    @DisplayName("Should save and retrieve weather forecast")
    void shouldSaveAndRetrieveWeatherForecast() {
        // Given
        WeatherForecast forecast = createWeatherForecast("Tokyo", LocalDate.now(), 28.0, 18.0);

        // When
        WeatherForecast saved = repository.save(forecast);
        Optional<WeatherForecast> retrieved = repository.findById(saved.getId());

        // Then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getCity()).isEqualTo("Tokyo");
        assertThat(retrieved.get().getHighTemp()).isEqualTo(28.0);
        assertThat(retrieved.get().getLowTemp()).isEqualTo(18.0);
    }

    @Test
    @DisplayName("Should find forecasts by city and date range")
    void shouldFindForecastsByCityAndDateRange() {
        // Given
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(2);

        // When
        List<WeatherForecast> forecasts = repository
                .findByCityAndForecastDateBetweenOrderByForecastDate("London", startDate, endDate);

        // Then
        assertThat(forecasts).hasSize(1);
        assertThat(forecasts.get(0).getCity()).isEqualTo("London");
        assertThat(forecasts.get(0).getForecastDate()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("Should find forecast by city and specific date")
    void shouldFindForecastByCityAndSpecificDate() {
        // When
        Optional<WeatherForecast> forecast = repository
                .findByCityAndForecastDate("London", LocalDate.now());

        // Then
        assertThat(forecast).isPresent();
        assertThat(forecast.get().getCity()).isEqualTo("London");
        assertThat(forecast.get().getForecastDate()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("Should find upcoming forecasts by city")
    void shouldFindUpcomingForecastsByCity() {
        // Given
        LocalDate startDate = LocalDate.now();

        // When
        List<WeatherForecast> forecasts = repository
                .findUpcomingForecastsByCity("London", startDate);

        // Then
        assertThat(forecasts).hasSize(1);
        assertThat(forecasts.get(0).getCity()).isEqualTo("London");
        assertThat(forecasts.get(0).getForecastDate()).isAfterOrEqualTo(startDate);
    }

    @Test
    @DisplayName("Should find forecasts by city ignoring case")
    void shouldFindForecastsByCityIgnoringCase() {
        // When
        List<WeatherForecast> forecasts = repository.findByCityIgnoreCase("LONDON");

        // Then
        assertThat(forecasts).hasSize(1);
        assertThat(forecasts.get(0).getCity()).isEqualTo("London");
    }

    @Test
    @DisplayName("Should return empty list for non-existent city")
    void shouldReturnEmptyListForNonExistentCity() {
        // When
        List<WeatherForecast> forecasts = repository
                .findByCityAndForecastDateBetweenOrderByForecastDate("NonExistentCity", 
                        LocalDate.now(), LocalDate.now().plusDays(1));

        // Then
        assertThat(forecasts).isEmpty();
    }

    @Test
    @DisplayName("Should delete forecasts by city and date before")
    void shouldDeleteForecastsByCityAndDateBefore() {
        // Given
        WeatherForecast oldForecast = createWeatherForecast("London", 
                LocalDate.now().minusDays(5), 20.0, 10.0);
        entityManager.persistAndFlush(oldForecast);

        // When
        repository.deleteByCityAndForecastDateBefore("London", LocalDate.now().minusDays(1));
        entityManager.flush();

        // Then
        List<WeatherForecast> remainingForecasts = repository.findByCityIgnoreCase("London");
        assertThat(remainingForecasts).hasSize(1);
        assertThat(remainingForecasts.get(0).getForecastDate()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("Should handle multiple forecasts for same city")
    void shouldHandleMultipleForecastsForSameCity() {
        // Given
        WeatherForecast londonTomorrow = createWeatherForecast("London", 
                LocalDate.now().plusDays(1), 23.0, 13.0);
        WeatherForecast londonDayAfter = createWeatherForecast("London", 
                LocalDate.now().plusDays(2), 21.0, 11.0);
        
        entityManager.persistAndFlush(londonTomorrow);
        entityManager.persistAndFlush(londonDayAfter);

        // When
        List<WeatherForecast> forecasts = repository
                .findByCityAndForecastDateBetweenOrderByForecastDate("London", 
                        LocalDate.now(), LocalDate.now().plusDays(3));

        // Then
        assertThat(forecasts).hasSize(3);
        assertThat(forecasts).extracting(WeatherForecast::getForecastDate)
                .isSorted();
    }

    @Test
    @DisplayName("Should update existing forecast")
    void shouldUpdateExistingForecast() {
        // Given
        WeatherForecast forecast = repository.findByCityIgnoreCase("London").get(0);
        Double newHighTemp = 30.0;

        // When
        forecast.setHighTemp(newHighTemp);
        forecast.setUpdatedAt(LocalDateTime.now());
        WeatherForecast updated = repository.save(forecast);

        // Then
        assertThat(updated.getHighTemp()).isEqualTo(newHighTemp);
        assertThat(updated.getUpdatedAt()).isAfter(updated.getCreatedAt());
    }

    @Test
    @DisplayName("Should handle special weather conditions")
    void shouldHandleSpecialWeatherConditions() {
        // Given
        WeatherForecast forecast = createWeatherForecast("Sydney", LocalDate.now(), 35.0, 25.0);
        forecast.setSpecialCondition("Heat wave warning");
        forecast.setWeatherCondition("Extreme Heat");

        // When
        WeatherForecast saved = repository.save(forecast);

        // Then
        assertThat(saved.getSpecialCondition()).isEqualTo("Heat wave warning");
        assertThat(saved.getWeatherCondition()).isEqualTo("Extreme Heat");
    }

    @Test
    @DisplayName("Should handle null and empty values gracefully")
    void shouldHandleNullAndEmptyValuesGracefully() {
        // Given
        WeatherForecast forecast = new WeatherForecast();
        forecast.setCity("TestCity");
        forecast.setForecastDate(LocalDate.now());
        forecast.setHighTemp(25.0);
        forecast.setLowTemp(15.0);
        // Leave optional fields as null

        // When
        WeatherForecast saved = repository.save(forecast);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    // Helper method
    private WeatherForecast createWeatherForecast(String city, LocalDate date, 
                                                 Double highTemp, Double lowTemp) {
        WeatherForecast forecast = new WeatherForecast();
        forecast.setCity(city);
        forecast.setForecastDate(date);
        forecast.setHighTemp(highTemp);
        forecast.setLowTemp(lowTemp);
        forecast.setDescription("Test weather");
        forecast.setWeatherCondition("Clear");
        forecast.setWindSpeed(5.0);
        forecast.setHumidity(65);
        forecast.setPressure(1013.25);
        forecast.setSpecialCondition("Test condition");
        return forecast;
    }
} 