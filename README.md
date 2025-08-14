# Weather Forecast Application

A full-stack weather forecast application built with **Angular**, **Spring Boot**, and **PostgreSQL**, containerized with **Docker** and featuring **comprehensive resilience patterns** for handling external API failures.

## 🌟 Features

- **3-day weather forecast** with high/low temperatures
- **Special weather conditions** with warnings:
  - High temperature (>40°C): "Use sunscreen lotion"
  - High wind speed (>10 m/s): "It's too windy, watch out!"
  - Rain conditions: "Carry umbrella" 
  - Thunderstorms: "Don't step out! A Storm is brewing!"
- **Offline mode support** with data caching
- **Resilience patterns** for external API failures:
  - **Circuit Breaker** - Prevents cascade failures
  - **Retry Logic** - Exponential backoff retry mechanism
  - **Rate Limiting** - API rate limiting protection
  - **Timeout Handling** - Configurable request timeouts
  - **Fallback Mechanisms** - Multiple fallback strategies
- **Responsive UI** built with Angular and Bootstrap
- **RESTful API** with OpenAPI documentation
- **Production-ready** with Docker containerization
- **CI/CD pipeline** with GitHub Actions
- **Security best practices** implemented
- **Comprehensive monitoring** and health checks

## 🛡️ Resilience Architecture

The application implements multiple resilience patterns to handle OpenWeatherMap API unavailability:

### Circuit Breaker Pattern
- **Failure Threshold**: 50% failure rate over 10 calls
- **Open State Duration**: 30 seconds
- **Half-Open Calls**: 3 test calls before closing
- **Automatic Recovery**: Self-healing when API recovers

### Retry Mechanism
- **Max Attempts**: 3 retries with exponential backoff
- **Initial Delay**: 1 second
- **Backoff Multiplier**: 2x (1s, 2s, 4s)
- **Retry Conditions**: Timeouts, I/O errors, server errors

### Rate Limiting
- **Request Limit**: 60 requests per minute
- **Timeout**: 5 seconds for rate limit wait
- **Buffer**: 100 events for monitoring

### Fallback Strategies

1. **Recent Cached Data** (< 30 minutes old)
2. **Older Cached Data** (< 24 hours old) - marked as offline
3. **Emergency Synthetic Data** - Last resort with warnings

### Monitoring & Observability
- **Health Checks**: `/actuator/health`, `/api/v1/weather/health`
- **Metrics**: Prometheus endpoints for circuit breaker, retry, and rate limiter
- **Status Endpoint**: `/api/v1/weather/status` - Detailed resilience status

## 🏗️ Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Angular UI    │    │  Spring Boot    │    │   PostgreSQL    │
│   (Frontend)    │◄──►│    (Backend)    │◄──►│   (Database)    │
│   Port: 80      │    │   Port: 8080    │    │   Port: 5432    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ OpenWeatherMap  │
                    │      API        │
                    │   (Resilient)   │
                    └─────────────────┘
```

## 🚀 Quick Start

### Prerequisites

- Docker and Docker Compose
- Node.js 18+ (for local development)
- Java 17+ (for local development)
- Git

### 1. Clone the Repository

```bash
git clone <repository-url>
cd WeatherApp
```

### 2. Environment Setup

Create a `.env` file in the root directory:

```bash
# OpenWeatherMap API Key (get from https://openweathermap.org/api)
OPENWEATHER_API_KEY=your_api_key_here

# Database credentials
DB_USERNAME=weatheruser
DB_PASSWORD=weatherpass

# Admin credentials
ADMIN_PASSWORD=admin123

# Resilience Configuration (optional)
OPENWEATHER_TIMEOUT=10000
OPENWEATHER_CONNECT_TIMEOUT=5000
```

### 3. Run with Docker Compose

```bash
# Build and start all services
docker-compose up --build

# Or run in detached mode
docker-compose up -d --build
```

### 4. Access the Application

- **Frontend**: http://localhost
- **Backend API**: http://localhost:8080
- **API Documentation**: http://localhost:8080/swagger-ui.html
- **Health Check**: http://localhost:8080/api/v1/weather/health
- **Service Status**: http://localhost:8080/api/v1/weather/status
- **Resilience Metrics**: http://localhost:8080/actuator/circuitbreakers

## 🛠️ Development Setup

### Backend Development

```bash
cd backend

# Run with Maven
./mvnw spring-boot:run

# Or with IDE (IntelliJ IDEA, Eclipse, VS Code)
# Import as Maven project and run WeatherServiceApplication.java
```

### Frontend Development

```bash
cd frontend

# Install dependencies
npm install

# Start development server
npm start

# Access at http://localhost:4200
```

### Database Development

```bash
# Start only PostgreSQL
docker-compose up postgres

# Connect to database
psql -h localhost -p 5432 -U weatheruser -d weatherdb
```

## 📡 API Endpoints

### Weather Forecast API

| Method | Endpoint | Description | Resilience |
|--------|----------|-------------|------------|
| GET | `/api/v1/weather/forecast?city={city}&offline={boolean}` | Get 3-day weather forecast | ✅ Full resilience |
| GET | `/api/v1/weather/offline/{city}` | Get cached weather data | ✅ Fallback only |
| GET | `/api/v1/weather/health` | Comprehensive health check | ✅ Circuit breaker aware |
| GET | `/api/v1/weather/status` | Detailed service status | ✅ Resilience metrics |

### Actuator Endpoints (Monitoring)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/actuator/health` | Spring Boot health |
| GET | `/actuator/circuitbreakers` | Circuit breaker status |
| GET | `/actuator/retries` | Retry attempts metrics |
| GET | `/actuator/ratelimiters` | Rate limiter status |
| GET | `/actuator/prometheus` | Prometheus metrics |

### Example API Response with Resilience Info

```json
{
  "city": "London",
  "forecasts": [
    {
      "city": "London",
      "forecastDate": "2024-01-15",
      "high_temp": 25.5,
      "low_temp": 15.3,
      "description": "Partly cloudy",
      "weather_condition": "Clouds",
      "wind_speed": 5.2,
      "humidity": 65,
      "pressure": 1013.25,
      "special_condition": "Have a great day!"
    }
  ],
  "offline_mode": false,
  "from_cache": false,
  "total_days": 3,
  "timestamp": "2024-01-15T12:00:00",
  "notice": "Live data from external API"
}
```

### Fallback Response Example

```json
{
  "city": "London",
  "forecasts": [
    {
      "special_condition": "⚠️ Offline data - External weather service temporarily unavailable"
    }
  ],
  "from_cache": true,
  "notice": "External weather service unavailable. Showing cached data."
}
```

## 🧪 Testing

### Backend Tests

```bash
cd backend
./mvnw test

# Test resilience patterns specifically
./mvnw test -Dtest=WeatherServiceResilienceTest

# With coverage report
./mvnw test jacoco:report
```

### Testing Resilience Patterns

```bash
# Test circuit breaker
curl "http://localhost:8080/actuator/circuitbreakers"

# Test with invalid API key to trigger fallback
curl "http://localhost:8080/api/v1/weather/forecast?city=London"

# Check retry metrics
curl "http://localhost:8080/actuator/retries"
```

### Frontend Tests

```bash
cd frontend

# Unit tests
npm test

# E2E tests
npm run e2e

# Linting
npm run lint
```

## 🔧 Configuration

### Resilience Configuration

Key resilience settings in `application.yml`:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      openweather-api:
        slidingWindowSize: 10          # Monitor last 10 calls
        failureRateThreshold: 50       # 50% failure rate triggers open
        waitDurationInOpenState: 30s   # Wait 30s before trying again
        
  retry:
    instances:
      openweather-api:
        maxAttempts: 3                 # Try up to 3 times
        waitDuration: 1s               # Start with 1s delay
        exponentialBackoffMultiplier: 2 # Double delay each retry
        
  ratelimiter:
    instances:
      openweather-api:
        limitForPeriod: 60             # 60 requests per period
        limitRefreshPeriod: 60s        # Refresh every minute
```

### Environment Variables

| Variable | Description | Default | Impact on Resilience |
|----------|-------------|---------|---------------------|
| `OPENWEATHER_API_KEY` | OpenWeatherMap API key | Required | API access |
| `OPENWEATHER_TIMEOUT` | Request timeout (ms) | 10000 | Circuit breaker triggers |
| `OPENWEATHER_CONNECT_TIMEOUT` | Connection timeout (ms) | 5000 | Health check timeouts |
| `weather.fallback.enabled` | Enable fallback mechanisms | true | Fallback behavior |
| `weather.fallback.data-age-threshold` | Max age for cached data (minutes) | 1440 | Cache validity |

## 📊 Monitoring and Observability

### Resilience Metrics

The application exposes comprehensive metrics for monitoring resilience patterns:

#### Circuit Breaker Metrics
- State transitions (CLOSED → OPEN → HALF_OPEN)
- Failure rates and success rates
- Call counts and response times

#### Retry Metrics
- Number of retry attempts
- Success rate after retries
- Retry delays and backoff progression

#### Rate Limiter Metrics
- Request rates and quotas
- Throttling events
- Available permits

### Health Checks

```bash
# Overall service health
curl http://localhost:8080/api/v1/weather/health

# Detailed status with resilience info
curl http://localhost:8080/api/v1/weather/status

# Spring Boot actuator health
curl http://localhost:8080/actuator/health
```

### Logging

Resilience events are logged with appropriate levels:
- **INFO**: State transitions, successful recoveries
- **WARN**: Circuit breaker opens, rate limiting triggered
- **ERROR**: Repeated failures, fallback activations
- **DEBUG**: Individual retry attempts, detailed metrics

## 🔒 Security

### Security Features Implemented

- **CORS Configuration** - Controlled cross-origin requests
- **Security Headers** - XSS, CSRF, Content-Type protection
- **Input Validation** - Request parameter validation
- **API Rate Limiting** - Built-in Spring Boot rate limiting
- **Secure Dependencies** - Regular security scans
- **Environment Variables** - Sensitive data protection
- **Non-root Containers** - Docker security best practices

## 🚀 Deployment

### Production Deployment with Resilience

For production deployment, ensure proper resilience configuration:

```yaml
# Production resilience settings
resilience4j:
  circuitbreaker:
    instances:
      openweather-api:
        slidingWindowSize: 20          # Larger window for production
        failureRateThreshold: 60       # Higher threshold
        waitDurationInOpenState: 60s   # Longer wait time
        
weather:
  fallback:
    enabled: true                      # Always enable fallback
    data-age-threshold: 2880          # 48 hours for production
```

### Monitoring in Production

1. **Set up alerts** for circuit breaker state changes
2. **Monitor retry metrics** for API health trends
3. **Track fallback usage** to identify API issues
4. **Configure log aggregation** for resilience events

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/resilience-improvement`)
3. Commit your changes (`git commit -m 'Add resilience feature'`)
4. Push to the branch (`git push origin feature/resilience-improvement`)
5. Open a Pull Request

### Resilience Development Guidelines

- **Test all failure scenarios** (API down, timeouts, rate limits)
- **Verify fallback mechanisms** work correctly
- **Monitor resource usage** during resilience testing
- **Document new resilience patterns** and configurations
- **Test circuit breaker state transitions**

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🛟 Support

- **Documentation**: Check this README and inline code comments
- **Issues**: Create GitHub issues for bugs and feature requests
- **Resilience Questions**: Use GitHub Discussions for resilience pattern questions
- **Monitoring**: Use Actuator endpoints for troubleshooting

## 🔄 Version History

- **v1.0.0** - Initial release with core weather forecast functionality
- **v1.1.0** - Added offline mode support
- **v1.2.0** - Enhanced UI/UX and responsive design
- **v1.3.0** - Added comprehensive testing and CI/CD
- **v2.0.0** - **🆕 Added comprehensive resilience patterns**
  - Circuit breaker implementation
  - Retry with exponential backoff
  - Rate limiting protection
  - Multiple fallback strategies
  - Enhanced monitoring and metrics

## 🙏 Acknowledgments

- **OpenWeatherMap API** for weather data
- **Spring Boot community** for excellent framework
- **Angular team** for powerful frontend framework
- **Docker** for containerization platform
- **Resilience4j** for resilience patterns implementation
- **Netflix OSS** for inspiring resilience patterns 