import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';

import { AppComponent } from './app.component';
import { WeatherService } from './services/weather.service';
import { CityService } from './services/city.service';
import { WeatherResponse, WeatherForecast } from './models/weather.model';
import { City, CitySearchResponse } from './models/city.model';

describe('AppComponent', () => {
  let component: AppComponent;
  let fixture: ComponentFixture<AppComponent>;
  let weatherService: jasmine.SpyObj<WeatherService>;
  let cityService: jasmine.SpyObj<CityService>;

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

  const mockCities: City[] = [
    {
      name: 'London',
      country: 'United Kingdom',
      countryCode: 'GB',
      state: 'England',
      latitude: 51.5074,
      longitude: -0.1278,
      displayName: 'London, England, GB'
    }
  ];

  beforeEach(async () => {
    const weatherServiceSpy = jasmine.createSpyObj('WeatherService', 
      ['getWeatherForecast', 'getOfflineWeatherData', 'setOfflineMode'], 
      {
        loading$: of(false),
        offlineMode$: of(false)
      }
    );

    const cityServiceSpy = jasmine.createSpyObj('CityService', 
      ['searchCities', 'getPopularCities'], 
      {
        loading$: of(false),
        error$: of(null),
        searchResults$: of([])
      }
    );

    await TestBed.configureTestingModule({
      declarations: [AppComponent],
      imports: [HttpClientTestingModule, FormsModule],
      providers: [
        { provide: WeatherService, useValue: weatherServiceSpy },
        { provide: CityService, useValue: cityServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AppComponent);
    component = fixture.componentInstance;
    weatherService = TestBed.inject(WeatherService) as jasmine.SpyObj<WeatherService>;
    cityService = TestBed.inject(CityService) as jasmine.SpyObj<CityService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Component Initialization', () => {
    it('should initialize with default values', () => {
      expect(component.currentCity).toBe('');
      expect(component.forecasts).toEqual([]);
      expect(component.citySuggestions).toEqual([]);
      expect(component.showSuggestions).toBeFalse();
      expect(component.selectedCityIndex).toBe(-1);
    });

    it('should load popular cities on init', () => {
      const popularResponse: CitySearchResponse = {
        cities: mockCities,
        total: 1,
        query: ''
      };
      cityService.getPopularCities.and.returnValue(of(popularResponse));

      component.ngOnInit();

      expect(cityService.getPopularCities).toHaveBeenCalled();
    });

    it('should set up theme updates on init', () => {
      spyOn(component, 'updateTimeBasedTheme');
      
      component.ngOnInit();

      expect(component.updateTimeBasedTheme).toHaveBeenCalled();
    });
  });

  describe('Weather Search', () => {
    it('should search weather for valid city', () => {
      weatherService.getWeatherForecast.and.returnValue(of(mockWeatherResponse));
      component.currentCity = 'London';

      component.searchWeather();

      expect(weatherService.getWeatherForecast).toHaveBeenCalledWith('London');
      expect(component.forecasts).toEqual(mockWeatherResponse.forecasts);
      expect(component.lastSearchResponse).toEqual(mockWeatherResponse);
    });

    it('should not search for empty city', () => {
      component.currentCity = '';

      component.searchWeather();

      expect(weatherService.getWeatherForecast).not.toHaveBeenCalled();
    });

    it('should handle weather search errors', () => {
      const errorMessage = 'City not found';
      weatherService.getWeatherForecast.and.returnValue(throwError(() => ({ message: errorMessage })));
      component.currentCity = 'InvalidCity';

      component.searchWeather();

      expect(component.error).toBe(errorMessage);
    });

    it('should search offline weather when in offline mode', () => {
      weatherService.getOfflineWeatherData.and.returnValue(of(mockWeatherResponse));
      component.currentCity = 'London';
      component.offlineMode$.next(true);

      component.searchWeather();

      expect(weatherService.getOfflineWeatherData).toHaveBeenCalledWith('London');
    });
  });

  describe('City Autocomplete', () => {
    it('should update suggestions on search input change', () => {
      const searchResponse: CitySearchResponse = {
        cities: mockCities,
        total: 1,
        query: 'Lon'
      };
      cityService.searchCities.and.returnValue(of(searchResponse));

      component.onSearchInputChange('Lon');

      expect(cityService.searchCities).toHaveBeenCalledWith('Lon');
    });

    it('should show suggestions on input focus', () => {
      component.citySuggestions = mockCities;

      component.onInputFocus();

      expect(component.showSuggestions).toBeTrue();
    });

    it('should hide suggestions on input blur with delay', (done) => {
      component.showSuggestions = true;

      component.onInputBlur();

      setTimeout(() => {
        expect(component.showSuggestions).toBeFalse();
        done();
      }, 250);
    });

    it('should handle keyboard navigation in suggestions', () => {
      component.citySuggestions = mockCities;
      component.showSuggestions = true;

      // Arrow down
      component.onKeyDown({ key: 'ArrowDown', preventDefault: () => {} } as KeyboardEvent);
      expect(component.selectedCityIndex).toBe(0);

      // Arrow up
      component.onKeyDown({ key: 'ArrowUp', preventDefault: () => {} } as KeyboardEvent);
      expect(component.selectedCityIndex).toBe(-1);

      // Enter key
      component.selectedCityIndex = 0;
      spyOn(component, 'selectCity');
      component.onKeyDown({ key: 'Enter', preventDefault: () => {} } as KeyboardEvent);
      expect(component.selectCity).toHaveBeenCalledWith(mockCities[0]);
    });

    it('should select city and search weather', () => {
      spyOn(component, 'searchWeather');
      const city = mockCities[0];

      component.selectCity(city);

      expect(component.currentCity).toBe(city.name);
      expect(component.showSuggestions).toBeFalse();
      expect(component.searchWeather).toHaveBeenCalled();
    });
  });

  describe('Theme Management', () => {
    it('should update theme based on weather condition', () => {
      component.forecasts = [
        { ...mockWeatherResponse.forecasts[0], weather_condition: 'Rain' }
      ];

      component.updateTheme();

      expect(component.currentWeatherType).toBe('rain');
      expect(component.backgroundClass).toContain('rain');
    });

    it('should determine if it is daytime', () => {
      const mockDate = new Date('2024-01-15T14:00:00'); // 2 PM
      spyOn(window, 'Date').and.returnValue(mockDate as any);

      component.updateTimeBasedTheme();

      expect(component.isDaytime).toBeTrue();
    });

    it('should determine if it is nighttime', () => {
      const mockDate = new Date('2024-01-15T22:00:00'); // 10 PM
      spyOn(window, 'Date').and.returnValue(mockDate as any);

      component.updateTimeBasedTheme();

      expect(component.isDaytime).toBeFalse();
    });

    it('should get correct weather icon for condition', () => {
      expect(component.getWeatherIcon('Clear')).toBe('☀️');
      expect(component.getWeatherIcon('Rain')).toBe('🌧️');
      expect(component.getWeatherIcon('Snow')).toBe('❄️');
      expect(component.getWeatherIcon('Clouds')).toBe('☁️');
    });

    it('should get correct alert class for special conditions', () => {
      expect(component.getAlertClass('Use sunscreen lotion')).toBe('alert-warning');
      expect(component.getAlertClass('Carry umbrella')).toBe('alert-info');
      expect(component.getAlertClass('Storm is brewing')).toBe('alert-danger');
    });
  });

  describe('Offline Mode', () => {
    it('should toggle offline mode', () => {
      component.toggleOfflineMode();

      expect(weatherService.setOfflineMode).toHaveBeenCalledWith(true);

      component.toggleOfflineMode();

      expect(weatherService.setOfflineMode).toHaveBeenCalledWith(false);
    });

    it('should clear forecasts when toggling offline mode', () => {
      component.forecasts = mockWeatherResponse.forecasts;

      component.toggleOfflineMode();

      expect(component.forecasts).toEqual([]);
      expect(component.error).toBe('');
    });
  });

  describe('Cached Cities', () => {
    it('should load cached cities from localStorage', () => {
      const cachedCities = ['London', 'Paris', 'Tokyo'];
      spyOn(localStorage, 'getItem').and.returnValue(JSON.stringify(cachedCities));

      component.loadCachedCities();

      expect(component.cachedCities).toEqual(cachedCities);
    });

    it('should save city to cache after successful search', () => {
      spyOn(localStorage, 'setItem');
      weatherService.getWeatherForecast.and.returnValue(of(mockWeatherResponse));
      component.currentCity = 'London';

      component.searchWeather();

      expect(localStorage.setItem).toHaveBeenCalledWith(
        'weatherApp_searchHistory',
        jasmine.any(String)
      );
    });

    it('should search weather for cached city', () => {
      spyOn(component, 'searchWeather');

      component.searchCachedCity('London');

      expect(component.currentCity).toBe('London');
      expect(component.searchWeather).toHaveBeenCalled();
    });
  });

  describe('Error Handling', () => {
    it('should display error message on API failure', () => {
      const errorMessage = 'Network error';
      weatherService.getWeatherForecast.and.returnValue(throwError(() => ({ message: errorMessage })));
      component.currentCity = 'London';

      component.searchWeather();

      expect(component.error).toBe(errorMessage);
      expect(component.forecasts).toEqual([]);
    });

    it('should clear error on successful search', () => {
      component.error = 'Previous error';
      weatherService.getWeatherForecast.and.returnValue(of(mockWeatherResponse));
      component.currentCity = 'London';

      component.searchWeather();

      expect(component.error).toBe('');
    });
  });

  describe('Component Cleanup', () => {
    it('should unsubscribe on destroy', () => {
      spyOn(component['destroy$'], 'next');
      spyOn(component['destroy$'], 'complete');

      component.ngOnDestroy();

      expect(component['destroy$'].next).toHaveBeenCalled();
      expect(component['destroy$'].complete).toHaveBeenCalled();
    });
  });

  describe('UI State Management', () => {
    it('should show loading state during weather search', () => {
      weatherService.loading$ = of(true);

      component.ngOnInit();

      component.loading$.subscribe(loading => {
        expect(loading).toBeTrue();
      });
    });

    it('should show city loading state during city search', () => {
      cityService.loading$ = of(true);

      component.ngOnInit();

      component.cityLoading$.subscribe(loading => {
        expect(loading).toBeTrue();
      });
    });

    it('should display city search errors', () => {
      const errorMessage = 'City search failed';
      cityService.error$ = of(errorMessage);

      component.ngOnInit();

      expect(component.cityError).toBe(errorMessage);
    });
  });

  describe('Responsive Behavior', () => {
    it('should handle window resize for theme updates', () => {
      spyOn(component, 'updateTheme');

      window.dispatchEvent(new Event('resize'));

      expect(component.updateTheme).toHaveBeenCalled();
    });

    it('should update theme on time changes', () => {
      spyOn(component, 'updateTimeBasedTheme');

      // Simulate time passing
      jasmine.clock().install();
      component.ngOnInit();
      jasmine.clock().tick(60000); // 1 minute

      expect(component.updateTimeBasedTheme).toHaveBeenCalled();

      jasmine.clock().uninstall();
    });
  });
}); 