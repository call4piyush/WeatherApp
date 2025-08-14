package com.weatherapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "City information for autocomplete")
public class CityDto {
    
    @Schema(description = "City name", example = "London")
    @JsonProperty("name")
    private String name;
    
    @Schema(description = "Country name", example = "United Kingdom")
    @JsonProperty("country")
    private String country;
    
    @Schema(description = "Country code", example = "GB")
    @JsonProperty("country_code")
    private String countryCode;
    
    @Schema(description = "State/Province name", example = "England")
    @JsonProperty("state")
    private String state;
    
    @Schema(description = "Latitude", example = "51.5074")
    @JsonProperty("latitude")
    private Double latitude;
    
    @Schema(description = "Longitude", example = "-0.1278")
    @JsonProperty("longitude")
    private Double longitude;
    
    @Schema(description = "Display name for UI", example = "London, England, United Kingdom")
    @JsonProperty("display_name")
    private String displayName;
    
    public CityDto() {}
    
    public CityDto(String name, String country, String countryCode, String state, Double latitude, Double longitude) {
        this.name = name;
        this.country = country;
        this.countryCode = countryCode;
        this.state = state;
        this.latitude = latitude;
        this.longitude = longitude;
        this.displayName = buildDisplayName();
    }
    
    private String buildDisplayName() {
        StringBuilder sb = new StringBuilder(name);
        if (state != null && !state.isEmpty()) {
            sb.append(", ").append(state);
        }
        if (country != null && !country.isEmpty()) {
            sb.append(", ").append(country);
        }
        return sb.toString();
    }
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
        this.displayName = buildDisplayName();
    }
    
    public String getCountry() {
        return country;
    }
    
    public void setCountry(String country) {
        this.country = country;
        this.displayName = buildDisplayName();
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
        this.displayName = buildDisplayName();
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
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        CityDto cityDto = (CityDto) obj;
        return name.equals(cityDto.name) && 
               country.equals(cityDto.country) &&
               countryCode.equals(cityDto.countryCode);
    }
    
    @Override
    public int hashCode() {
        return java.util.Objects.hash(name, country, countryCode);
    }
} 