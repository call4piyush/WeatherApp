package com.weatherapp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI weatherServiceOpenAPI() {
        Server devServer = new Server();
        devServer.setUrl("http://localhost:8080");
        devServer.setDescription("Server URL in Development environment");
        
        Server prodServer = new Server();
        prodServer.setUrl("https://weather-api.example.com");
        prodServer.setDescription("Server URL in Production environment");
        
        Contact contact = new Contact();
        contact.setEmail("developer@weatherapp.com");
        contact.setName("Weather App Team");
        contact.setUrl("https://www.weatherapp.com");
        
        License mitLicense = new License().name("MIT License").url("https://choosealicense.com/licenses/mit/");
        
        Info info = new Info()
                .title("Weather Forecast API")
                .version("1.0.0")
                .contact(contact)
                .description("This API provides 3-day weather forecast for cities with special conditions and warnings.")
                .termsOfService("https://www.weatherapp.com/terms")
                .license(mitLicense);
        
        return new OpenAPI().info(info).servers(List.of(devServer, prodServer));
    }
} 