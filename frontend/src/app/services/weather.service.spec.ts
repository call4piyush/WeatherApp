import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { WeatherService } from './weather.service';
import { WeatherResponse, WeatherForecast } from '../models/weather.model';
import { environment } from '../../environments/environment';

describe('WeatherService', () => {
  let service: WeatherService;
  let httpMock: HttpTestingController;

  const mockWeatherResponse: WeatherResponse = {
    city: 'London',
    forecasts: [
      {
        city: 'London',
        forecastDate: '2024-01-15',
        high_temp: 25.5,
        low_temp: 15.3,
        description: 'Partly cloudy',
        weather_condition: 'Clouds',
        wind_speed: 5.2,
        humidity: 65,
        pressure: 1013.25,
        special_condition: 'Have a great day!'
      }
    ],
    offline_mode: false,
    from_cache: false,
    total_days: 3,
    timestamp: '2024-01-15T12:00:00',
    notice: 'Live data from external API'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [WeatherService]
    });
    service = TestBed.inject(WeatherService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getWeatherForecast', () => {
    it('should fetch weather forecast for a city', () => {
      const city = 'London';

      service.getWeatherForecast(city).subscribe(response => {
        expect(response).toEqual(mockWeatherResponse);
        expect(response.city).toBe(city);
        expect(response.forecasts).toHaveSize(1);
        expect(response.offline_mode).toBeFalse();
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/weather/forecast?city=${city}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockWeatherResponse);
    });

    it('should handle HTTP errors gracefully', () => {
      const city = 'InvalidCity';
      const errorMessage = 'City not found';

      service.getWeatherForecast(city).subscribe({
        next: () => fail('Expected an error'),
        error: (error) => {
          expect(error.status).toBe(404);
        }
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/weather/forecast?city=${city}`);
      req.flush({ message: errorMessage }, { status: 404, statusText: 'Not Found' });
    });

    it('should handle network errors', () => {
      const city = 'London';

      service.getWeatherForecast(city).subscribe({
        next: () => fail('Expected an error'),
        error: (error) => {
          expect(error.name).toBe('HttpErrorResponse');
        }
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/weather/forecast?city=${city}`);
      req.error(new ErrorEvent('Network error'));
    });

    it('should handle empty city parameter', () => {
      service.getWeatherForecast('').subscribe({
        next: () => fail('Expected an error'),
        error: (error) => {
          expect(error).toBeDefined();
        }
      });

      httpMock.expectNone(`${environment.apiUrl}/weather/forecast?city=`);
    });
  });

  describe('getOfflineWeatherData', () => {
    it('should fetch offline weather data', () => {
      const city = 'London';
      const offlineResponse = { ...mockWeatherResponse, offline_mode: true };

      service.getOfflineWeatherData(city).subscribe(response => {
        expect(response).toEqual(offlineResponse);
        expect(response.offline_mode).toBeTrue();
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/weather/offline?city=${city}`);
      expect(req.request.method).toBe('GET');
      req.flush(offlineResponse);
    });
  });

  describe('loading state', () => {
    it('should update loading state during API calls', () => {
      const city = 'London';
      let loadingStates: boolean[] = [];

      service.loading$.subscribe(loading => {
        loadingStates.push(loading);
      });

      service.getWeatherForecast(city).subscribe();

      const req = httpMock.expectOne(`${environment.apiUrl}/weather/forecast?city=${city}`);
      req.flush(mockWeatherResponse);

      expect(loadingStates).toContain(true);
      expect(loadingStates).toContain(false);
    });
  });

  describe('offline mode', () => {
    it('should update offline mode state', () => {
      let offlineModeStates: boolean[] = [];

      service.offlineMode$.subscribe(offline => {
        offlineModeStates.push(offline);
      });

      service.setOfflineMode(true);
      service.setOfflineMode(false);

      expect(offlineModeStates).toContain(true);
      expect(offlineModeStates).toContain(false);
    });

    it('should persist offline mode in localStorage', () => {
      spyOn(localStorage, 'setItem');

      service.setOfflineMode(true);

      expect(localStorage.setItem).toHaveBeenCalledWith('weatherApp_offlineMode', 'true');
    });

    it('should load offline mode from localStorage on init', () => {
      spyOn(localStorage, 'getItem').and.returnValue('true');

      const newService = new WeatherService(TestBed.inject(HttpClientTestingModule) as any);

      newService.offlineMode$.subscribe(offline => {
        expect(offline).toBeTrue();
      });
    });
  });

  describe('error handling', () => {
    it('should handle server errors (5xx)', () => {
      const city = 'London';

      service.getWeatherForecast(city).subscribe({
        next: () => fail('Expected an error'),
        error: (error) => {
          expect(error.status).toBe(500);
        }
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/weather/forecast?city=${city}`);
      req.flush({ message: 'Internal Server Error' }, { status: 500, statusText: 'Internal Server Error' });
    });

    it('should handle client errors (4xx)', () => {
      const city = 'London';

      service.getWeatherForecast(city).subscribe({
        next: () => fail('Expected an error'),
        error: (error) => {
          expect(error.status).toBe(400);
        }
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/weather/forecast?city=${city}`);
      req.flush({ message: 'Bad Request' }, { status: 400, statusText: 'Bad Request' });
    });
  });

  describe('caching', () => {
    it('should cache successful responses', () => {
      const city = 'London';

      // First call
      service.getWeatherForecast(city).subscribe();
      const req1 = httpMock.expectOne(`${environment.apiUrl}/weather/forecast?city=${city}`);
      req1.flush(mockWeatherResponse);

      // Second call should use cache (no HTTP request)
      service.getWeatherForecast(city).subscribe(response => {
        expect(response).toEqual(mockWeatherResponse);
      });

      httpMock.expectNone(`${environment.apiUrl}/weather/forecast?city=${city}`);
    });

    it('should clear cache when offline mode changes', () => {
      const city = 'London';

      // First call
      service.getWeatherForecast(city).subscribe();
      const req1 = httpMock.expectOne(`${environment.apiUrl}/weather/forecast?city=${city}`);
      req1.flush(mockWeatherResponse);

      // Change offline mode
      service.setOfflineMode(true);

      // Second call should make new HTTP request
      service.getWeatherForecast(city).subscribe();
      httpMock.expectNone(`${environment.apiUrl}/weather/forecast?city=${city}`);
    });
  });

  describe('special weather conditions', () => {
    it('should handle extreme weather conditions', () => {
      const city = 'Phoenix';
      const extremeWeatherResponse: WeatherResponse = {
        ...mockWeatherResponse,
        city: 'Phoenix',
        forecasts: [{
          ...mockWeatherResponse.forecasts[0],
          city: 'Phoenix',
          high_temp: 45.0,
          special_condition: 'Extreme heat warning - Use sunscreen lotion'
        }]
      };

      service.getWeatherForecast(city).subscribe(response => {
        expect(response.forecasts[0].special_condition).toContain('Extreme heat warning');
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/weather/forecast?city=${city}`);
      req.flush(extremeWeatherResponse);
    });
  });

  describe('concurrent requests', () => {
    it('should handle multiple concurrent requests', () => {
      const cities = ['London', 'Paris', 'Tokyo'];
      const responses: WeatherResponse[] = [];

      cities.forEach(city => {
        service.getWeatherForecast(city).subscribe(response => {
          responses.push(response);
        });
      });

      cities.forEach(city => {
        const req = httpMock.expectOne(`${environment.apiUrl}/weather/forecast?city=${city}`);
        req.flush({ ...mockWeatherResponse, city });
      });

      expect(responses).toHaveSize(3);
      expect(responses.map(r => r.city)).toEqual(cities);
    });
  });
}); 