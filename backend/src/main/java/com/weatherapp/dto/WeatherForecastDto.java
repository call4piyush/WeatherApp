package com.weatherapp.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Weather forecast data transfer object")
public class WeatherForecastDto {
    
    @Schema(description = "City name", example = "London")
    private String city;
    
    @Schema(description = "Forecast date", example = "2024-01-15")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate forecastDate;
    
    @Schema(description = "High temperature in Celsius", example = "25.5")
    @JsonProperty("high_temp")
    private Double highTemp;
    
    @Schema(description = "Low temperature in Celsius", example = "15.3")
    @JsonProperty("low_temp")
    private Double lowTemp;
    
    @Schema(description = "Weather description", example = "Partly cloudy")
    private String description;
    
    @Schema(description = "Weather condition", example = "Clouds")
    @JsonProperty("weather_condition")
    private String weatherCondition;
    
    @Schema(description = "Wind speed in m/s", example = "5.2")
    @JsonProperty("wind_speed")
    private Double windSpeed;
    
    @Schema(description = "Humidity percentage", example = "65")
    private Integer humidity;
    
    @Schema(description = "Atmospheric pressure in hPa", example = "1013.25")
    private Double pressure;
    
    @Schema(description = "Special weather conditions or warnings", 
            example = "Carry umbrella")
    @JsonProperty("special_condition")
    private String specialCondition;
    
    // Constructors
    public WeatherForecastDto() {}
    
    public WeatherForecastDto(String city, LocalDate forecastDate, 
                             Double highTemp, Double lowTemp, String description) {
        this.city = city;
        this.forecastDate = forecastDate;
        this.highTemp = highTemp;
        this.lowTemp = lowTemp;
        this.description = description;
    }
    
    // Getters and Setters
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
} 