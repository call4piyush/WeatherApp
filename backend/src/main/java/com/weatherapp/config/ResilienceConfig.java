package com.weatherapp.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResilienceConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(ResilienceConfig.class);
    
    @Bean
    public RegistryEventConsumer<CircuitBreaker> circuitBreakerEventConsumer() {
        return new RegistryEventConsumer<CircuitBreaker>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<CircuitBreaker> entryAddedEvent) {
                CircuitBreaker circuitBreaker = entryAddedEvent.getAddedEntry();
                logger.info("Circuit breaker {} added", circuitBreaker.getName());
                
                circuitBreaker.getEventPublisher()
                    .onStateTransition(event -> {
                        logger.info("Circuit breaker {} transitioned from {} to {}", 
                            circuitBreaker.getName(), event.getStateTransition().getFromState(), 
                            event.getStateTransition().getToState());
                    })
                    .onFailureRateExceeded(event -> {
                        logger.warn("Circuit breaker {} failure rate exceeded: {}%", 
                            circuitBreaker.getName(), event.getFailureRate());
                    })
                    .onCallNotPermitted(event -> {
                        logger.warn("Circuit breaker {} call not permitted", circuitBreaker.getName());
                    })
                    .onError(event -> {
                        logger.error("Circuit breaker {} error: {}", 
                            circuitBreaker.getName(), event.getThrowable().getMessage());
                    });
            }
            
            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<CircuitBreaker> entryRemoveEvent) {
                logger.info("Circuit breaker {} removed", entryRemoveEvent.getRemovedEntry().getName());
            }
            
            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<CircuitBreaker> entryReplacedEvent) {
                logger.info("Circuit breaker {} replaced", entryReplacedEvent.getNewEntry().getName());
            }
        };
    }
    
    @Bean
    public RegistryEventConsumer<Retry> retryEventConsumer() {
        return new RegistryEventConsumer<Retry>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<Retry> entryAddedEvent) {
                Retry retry = entryAddedEvent.getAddedEntry();
                logger.info("Retry {} added", retry.getName());
                
                retry.getEventPublisher()
                    .onRetry(event -> {
                        logger.debug("Retry {} attempt {} for {}", 
                            retry.getName(), event.getNumberOfRetryAttempts(), 
                            event.getLastThrowable().getMessage());
                    })
                    .onSuccess(event -> {
                        logger.debug("Retry {} succeeded after {} attempts", 
                            retry.getName(), event.getNumberOfRetryAttempts());
                    })
                    .onError(event -> {
                        logger.error("Retry {} failed after {} attempts: {}", 
                            retry.getName(), event.getNumberOfRetryAttempts(),
                            event.getLastThrowable().getMessage());
                    });
            }
            
            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<Retry> entryRemoveEvent) {
                logger.info("Retry {} removed", entryRemoveEvent.getRemovedEntry().getName());
            }
            
            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<Retry> entryReplacedEvent) {
                logger.info("Retry {} replaced", entryReplacedEvent.getNewEntry().getName());
            }
        };
    }
} 