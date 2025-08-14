package com.weatherapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "weather_forecasts")
public class WeatherForecast {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Column(nullable = false)
    private String city;
    
    @NotNull
    @Column(nullable = false)
    private LocalDate forecastDate;
    
    @NotNull
    @Column(nullable = false)
    private Double highTemp;
    
    @NotNull
    @Column(nullable = false)
    private Double lowTemp;
    
    @Column
    private String description;
    
    @Column
    private String weatherCondition;
    
    @Column
    private Double windSpeed;
    
    @Column
    private Integer humidity;
    
    @Column
    private Double pressure;
    
    @Column
    private String specialCondition; // For rain, wind warnings, etc.
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    // Constructors
    public WeatherForecast() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public WeatherForecast(String city, LocalDate forecastDate, Double highTemp, 
                          Double lowTemp, String description) {
        this();
        this.city = city;
        this.forecastDate = forecastDate;
        this.highTemp = highTemp;
        this.lowTemp = lowTemp;
        this.description = description;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getCity() {
        return city;
    }
    
    public void setCity(String city) {
        this.city = city;
    }
    
    public LocalDate getForecastDate() {
        return forecastDate;
    }
    
    public void setForecastDate(LocalDate forecastDate) {
        this.forecastDate = forecastDate;
    }
    
    public Double getHighTemp() {
        return highTemp;
    }
    
    public void setHighTemp(Double highTemp) {
        this.highTemp = highTemp;
    }
    
    public Double getLowTemp() {
        return lowTemp;
    }
    
    public void setLowTemp(Double lowTemp) {
        this.lowTemp = lowTemp;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getWeatherCondition() {
        return weatherCondition;
    }
    
    public void setWeatherCondition(String weatherCondition) {
        this.weatherCondition = weatherCondition;
    }
    
    public Double getWindSpeed() {
        return windSpeed;
    }
    
    public void setWindSpeed(Double windSpeed) {
        this.windSpeed = windSpeed;
    }
    
    public Integer getHumidity() {
        return humidity;
    }
    
    public void setHumidity(Integer humidity) {
        this.humidity = humidity;
    }
    
    public Double getPressure() {
        return pressure;
    }
    
    public void setPressure(Double pressure) {
        this.pressure = pressure;
    }
    
    public String getSpecialCondition() {
        return specialCondition;
    }
    
    public void setSpecialCondition(String specialCondition) {
        this.specialCondition = specialCondition;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
} 