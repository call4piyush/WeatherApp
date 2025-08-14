import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, Observable, BehaviorSubject } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap, takeUntil } from 'rxjs/operators';

import { WeatherService } from './services/weather.service';
import { CityService } from './services/city.service';
import { WeatherForecast, WeatherResponse } from './models/weather.model';
import { City } from './models/city.model';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit, OnDestroy {
  title = 'weather-app-frontend';
  searchCity = '';
  currentCity = '';
  forecasts: WeatherForecast[] = [];
  lastSearchResponse: WeatherResponse | null = null;
  
  // City autocomplete properties
  searchInput$ = new Subject<string>();
  citySuggestions: City[] = [];
  showSuggestions = false;
  selectedCityIndex = -1;
  
  // Loading and error states
  loading$ = new BehaviorSubject<boolean>(false);
  cityLoading$ = new BehaviorSubject<boolean>(false);
  error: string | null = null;
  cityError: string | null = null;
  offlineMode$ = new BehaviorSubject<boolean>(false);
  
  // Cached cities for quick access
  cachedCities: string[] = [];
  
  // Dynamic theme properties
  currentTheme = 'day-clear';
  backgroundClass = 'bg-day-clear';
  isDaytime = true;
  currentWeatherType = 'clear';
  
  private destroy$ = new Subject<void>();

  constructor(
    private weatherService: WeatherService,
    private cityService: CityService
  ) {
    this.updateTimeBasedTheme();
  }

  ngOnInit() {
    this.setupCitySearch();
    this.loadCachedCities();
    this.updateTimeBasedTheme();
    
    // Update theme every minute
    setInterval(() => {
      this.updateTimeBasedTheme();
    }, 60000);
    
    // Subscribe to service observables
    this.weatherService.offlineMode$.pipe(takeUntil(this.destroy$))
      .subscribe(offline => this.offlineMode$.next(offline));
    
    this.weatherService.loading$.pipe(takeUntil(this.destroy$))
      .subscribe(loading => this.loading$.next(loading));
    
    this.cityService.searchResults$.pipe(takeUntil(this.destroy$))
      .subscribe(cities => {
        this.citySuggestions = cities;
        this.showSuggestions = cities.length > 0;
      });
    
    this.cityService.loading$.pipe(takeUntil(this.destroy$))
      .subscribe(loading => this.cityLoading$.next(loading));
    
    this.cityService.error$.pipe(takeUntil(this.destroy$))
      .subscribe(error => this.cityError = error);
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private setupCitySearch() {
    this.cityService.createDebouncedSearch(this.searchInput$)
      .pipe(takeUntil(this.destroy$))
      .subscribe();
  }

  onSearchInputChange() {
    this.searchInput$.next(this.searchCity);
  }

  onInputFocus() {
    if (this.citySuggestions.length > 0) {
      this.showSuggestions = true;
    }
  }

  onInputBlur() {
    // Delay hiding to allow for clicks on suggestions
    setTimeout(() => {
      this.showSuggestions = false;
    }, 200);
  }

  onKeyDown(event: KeyboardEvent) {
    if (!this.showSuggestions) return;

    switch (event.key) {
      case 'ArrowDown':
        event.preventDefault();
        this.selectedCityIndex = Math.min(
          this.selectedCityIndex + 1,
          this.citySuggestions.length - 1
        );
        break;
      case 'ArrowUp':
        event.preventDefault();
        this.selectedCityIndex = Math.max(this.selectedCityIndex - 1, -1);
        break;
      case 'Enter':
        event.preventDefault();
        if (this.selectedCityIndex >= 0) {
          this.selectCity(this.citySuggestions[this.selectedCityIndex]);
        } else {
          this.searchWeather();
        }
        break;
      case 'Escape':
        this.showSuggestions = false;
        this.selectedCityIndex = -1;
        break;
    }
  }

  selectCity(city: City) {
    this.searchCity = city.name;
    this.currentCity = city.name;
    this.showSuggestions = false;
    this.selectedCityIndex = -1;
    this.searchWeather();
  }

  searchWeather() {
    if (!this.searchCity.trim()) return;

    this.error = null;
    this.currentCity = this.searchCity;
    this.addToCachedCities(this.searchCity);

    if (this.offlineMode$.value) {
      this.getOfflineWeather();
    } else {
      this.getOnlineWeather();
    }
  }

  updateTimeBasedTheme() {
    const now = new Date();
    const hours = now.getHours();
    
    // Determine if it's daytime (6 AM to 6 PM)
    this.isDaytime = hours >= 6 && hours < 18;
    
    // Update theme based on weather and time
    this.updateTheme();
  }

  updateTheme() {
    if (this.forecasts.length > 0) {
      const currentWeather = this.forecasts[0];
      this.currentWeatherType = this.getWeatherType(currentWeather.description);
    } else {
      this.currentWeatherType = 'clear';
    }
    
    const timePrefix = this.isDaytime ? 'day' : 'night';
    this.currentTheme = `${timePrefix}-${this.currentWeatherType}`;
    this.backgroundClass = `bg-${this.currentTheme}`;
  }

  getWeatherType(description: string): string {
    const desc = description.toLowerCase();
    
    if (desc.includes('rain') || desc.includes('drizzle')) return 'rain';
    if (desc.includes('storm') || desc.includes('thunder')) return 'storm';
    if (desc.includes('snow') || desc.includes('blizzard')) return 'snow';
    if (desc.includes('dust') || desc.includes('sand')) return 'dust';
    if (desc.includes('fog') || desc.includes('mist') || desc.includes('haze')) return 'fog';
    if (desc.includes('cloud')) return 'cloudy';
    if (desc.includes('wind')) return 'windy';
    
    return 'clear';
  }

  getThemeColors(): any {
    const themes: {[key: string]: any} = {
      'day-clear': { primary: '#FFD700', secondary: '#87CEEB', accent: '#FFA500' },
      'night-clear': { primary: '#4A90E2', secondary: '#2C3E50', accent: '#F39C12' },
      'day-rain': { primary: '#6C7B7F', secondary: '#34495E', accent: '#3498DB' },
      'night-rain': { primary: '#2C3E50', secondary: '#34495E', accent: '#3498DB' },
      'day-storm': { primary: '#4A4A4A', secondary: '#2C2C2C', accent: '#9B59B6' },
      'night-storm': { primary: '#1A1A1A', secondary: '#000000', accent: '#8E44AD' },
      'day-snow': { primary: '#E8F4FD', secondary: '#BDC3C7', accent: '#3498DB' },
      'night-snow': { primary: '#34495E', secondary: '#2C3E50', accent: '#85C1E9' },
      'day-dust': { primary: '#D4AF37', secondary: '#CD853F', accent: '#DEB887' },
      'night-dust': { primary: '#8B4513', secondary: '#A0522D', accent: '#F4A460' },
      'day-fog': { primary: '#D3D3D3', secondary: '#A9A9A9', accent: '#708090' },
      'night-fog': { primary: '#696969', secondary: '#2F4F4F', accent: '#778899' },
      'day-cloudy': { primary: '#87CEEB', secondary: '#B0C4DE', accent: '#4682B4' },
      'night-cloudy': { primary: '#2F4F4F', secondary: '#36454F', accent: '#5F9EA0' },
      'day-windy': { primary: '#87CEEB', secondary: '#B0E0E6', accent: '#00CED1' },
      'night-windy': { primary: '#2F4F4F', secondary: '#36454F', accent: '#4682B4' }
    };
    
    return themes[this.currentTheme] || themes['day-clear'];
  }

  private getOnlineWeather() {
    this.weatherService.getWeatherForecast(this.searchCity, false).subscribe({
      next: (response: any) => {
        this.lastSearchResponse = {
          city: response.city,
          forecasts: response.forecasts,
          offline_mode: response.offline_mode,
          from_cache: response.from_cache,
          timestamp: response.timestamp,
          total_days: response.total_days || 3
        };
        this.forecasts = response.forecasts;
        this.error = null;
        
        // Update theme based on new weather data
        this.updateTheme();
      },
      error: (err: any) => {
        this.error = err.message || 'Failed to fetch weather data';
        this.forecasts = [];
      }
    });
  }

  private getOfflineWeather() {
    this.weatherService.getOfflineWeatherData(this.searchCity).subscribe({
      next: (response: any) => {
        this.forecasts = response.forecasts || [];
        this.lastSearchResponse = {
          city: this.searchCity,
          forecasts: response.forecasts || [],
          offline_mode: true,
          from_cache: true,
          timestamp: new Date().toISOString(),
          total_days: 3
        };
        this.error = null;
        
        // Update theme based on new weather data
        this.updateTheme();
      },
      error: (err: any) => {
        this.error = err.message || 'No cached data available';
        this.forecasts = [];
      }
    });
  }

  toggleOfflineMode() {
    const newMode = !this.offlineMode$.value;
    this.offlineMode$.next(newMode);
    this.weatherService.setOfflineMode(newMode);
  }

  private loadCachedCities() {
    const cached = localStorage.getItem('cachedCities');
    if (cached) {
      this.cachedCities = JSON.parse(cached);
    }
  }

  private addToCachedCities(city: string) {
    if (!this.cachedCities.includes(city)) {
      this.cachedCities.unshift(city);
      this.cachedCities = this.cachedCities.slice(0, 5); // Keep only 5
      localStorage.setItem('cachedCities', JSON.stringify(this.cachedCities));
    }
  }

  getWeatherIcon(description: string): string {
    const desc = description.toLowerCase();
    if (desc.includes('clear')) return '☀️';
    if (desc.includes('cloud')) return '☁️';
    if (desc.includes('rain')) return '🌧️';
    if (desc.includes('storm')) return '⛈️';
    if (desc.includes('snow')) return '❄️';
    if (desc.includes('mist') || desc.includes('fog')) return '🌫️';
    if (desc.includes('wind')) return '💨';
    return '🌤️';
  }

  getAlertClass(fromCache: boolean): string {
    return fromCache ? 'alert-warning' : 'alert-success';
  }
} 