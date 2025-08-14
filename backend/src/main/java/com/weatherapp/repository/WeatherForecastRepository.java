package com.weatherapp.repository;

import com.weatherapp.model.WeatherForecast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeatherForecastRepository extends JpaRepository<WeatherForecast, Long> {
    
    List<WeatherForecast> findByCityAndForecastDateBetweenOrderByForecastDate(
            String city, LocalDate startDate, LocalDate endDate);
    
    Optional<WeatherForecast> findByCityAndForecastDate(String city, LocalDate forecastDate);
    
    @Query("SELECT w FROM WeatherForecast w WHERE w.city = :city AND w.forecastDate >= :startDate ORDER BY w.forecastDate")
    List<WeatherForecast> findUpcomingForecastsByCity(
            @Param("city") String city, @Param("startDate") LocalDate startDate);
    
    void deleteByCityAndForecastDateBefore(String city, LocalDate date);
    
    List<WeatherForecast> findByCityIgnoreCase(String city);
} 