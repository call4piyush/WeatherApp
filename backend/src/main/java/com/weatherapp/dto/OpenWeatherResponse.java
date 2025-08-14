package com.weatherapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class OpenWeatherResponse {
    
    @JsonProperty("list")
    private List<ForecastItem> forecastList;
    
    private City city;
    
    public List<ForecastItem> getForecastList() {
        return forecastList;
    }
    
    public void setForecastList(List<ForecastItem> forecastList) {
        this.forecastList = forecastList;
    }
    
    public City getCity() {
        return city;
    }
    
    public void setCity(City city) {
        this.city = city;
    }
    
    public static class ForecastItem {
        private Main main;
        private List<Weather> weather;
        private Wind wind;
        private Rain rain;
        
        @JsonProperty("dt_txt")
        private String dateTime;
        
        public Main getMain() {
            return main;
        }
        
        public void setMain(Main main) {
            this.main = main;
        }
        
        public List<Weather> getWeather() {
            return weather;
        }
        
        public void setWeather(List<Weather> weather) {
            this.weather = weather;
        }
        
        public Wind getWind() {
            return wind;
        }
        
        public void setWind(Wind wind) {
            this.wind = wind;
        }
        
        public Rain getRain() {
            return rain;
        }
        
        public void setRain(Rain rain) {
            this.rain = rain;
        }
        
        public String getDateTime() {
            return dateTime;
        }
        
        public void setDateTime(String dateTime) {
            this.dateTime = dateTime;
        }
    }
    
    public static class Main {
        private Double temp;
        
        @JsonProperty("temp_min")
        private Double tempMin;
        
        @JsonProperty("temp_max")
        private Double tempMax;
        
        private Integer humidity;
        private Double pressure;
        
        public Double getTemp() {
            return temp;
        }
        
        public void setTemp(Double temp) {
            this.temp = temp;
        }
        
        public Double getTempMin() {
            return tempMin;
        }
        
        public void setTempMin(Double tempMin) {
            this.tempMin = tempMin;
        }
        
        public Double getTempMax() {
            return tempMax;
        }
        
        public void setTempMax(Double tempMax) {
            this.tempMax = tempMax;
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
    }
    
    public static class Weather {
        private String main;
        private String description;
        
        public String getMain() {
            return main;
        }
        
        public void setMain(String main) {
            this.main = main;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
    }
    
    public static class Wind {
        private Double speed;
        
        public Double getSpeed() {
            return speed;
        }
        
        public void setSpeed(Double speed) {
            this.speed = speed;
        }
    }
    
    public static class Rain {
        @JsonProperty("3h")
        private Double threeHour;
        
        public Double getThreeHour() {
            return threeHour;
        }
        
        public void setThreeHour(Double threeHour) {
            this.threeHour = threeHour;
        }
    }
    
    public static class City {
        private String name;
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
    }
} 