package com.weatherapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GeocodingResponse {
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("local_names")
    private LocalNames localNames;
    
    @JsonProperty("lat")
    private Double latitude;
    
    @JsonProperty("lon")
    private Double longitude;
    
    @JsonProperty("country")
    private String countryCode;
    
    @JsonProperty("state")
    private String state;
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public LocalNames getLocalNames() {
        return localNames;
    }
    
    public void setLocalNames(LocalNames localNames) {
        this.localNames = localNames;
    }
    
    public Double getLatitude() {
        return latitude;
    }
    
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }
    
    public Double getLongitude() {
        return longitude;
    }
    
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
    
    public String getCountryCode() {
        return countryCode;
    }
    
    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }
    
    public String getState() {
        return state;
    }
    
    public void setState(String state) {
        this.state = state;
    }
    
    public static class LocalNames {
        @JsonProperty("en")
        private String english;
        
        @JsonProperty("local")
        private String local;
        
        public String getEnglish() {
            return english;
        }
        
        public void setEnglish(String english) {
            this.english = english;
        }
        
        public String getLocal() {
            return local;
        }
        
        public void setLocal(String local) {
            this.local = local;
        }
    }
} 