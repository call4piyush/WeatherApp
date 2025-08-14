import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Observable, BehaviorSubject, throwError, of } from 'rxjs';
import { catchError, tap, map } from 'rxjs/operators';
import { WeatherResponse, WeatherForecast, WeatherError } from '../models/weather.model';

@Injectable({
  providedIn: 'root'
})
export class WeatherService {
  private readonly baseUrl = 'http://localhost:8080/api/v1/weather';
  private readonly storageKey = 'weather_cache';
  
  private offlineModeSubject = new BehaviorSubject<boolean>(false);
  public offlineMode$ = this.offlineModeSubject.asObservable();
  
  private loadingSubject = new BehaviorSubject<boolean>(false);
  public loading$ = this.loadingSubject.asObservable();

  constructor(private http: HttpClient) {
    // Check if we're offline
    this.checkOnlineStatus();
    
    // Listen for online/offline events
    window.addEventListener('online', () => this.setOfflineMode(false));
    window.addEventListener('offline', () => this.setOfflineMode(true));
  }

  private checkOnlineStatus(): void {
    this.setOfflineMode(!navigator.onLine);
  }

  setOfflineMode(isOffline: boolean): void {
    this.offlineModeSubject.next(isOffline);
  }

  getWeatherForecast(city: string, forceOffline: boolean = false): Observable<WeatherResponse> {
    this.loadingSubject.next(true);
    
    const isOffline = this.offlineModeSubject.value || forceOffline;
    
    if (isOffline) {
      return this.getOfflineWeatherData(city);
    }

    const params = new HttpParams()
      .set('city', city)
      .set('offline', 'false');

    return this.http.get<WeatherResponse>(`${this.baseUrl}/forecast`, { params })
      .pipe(
        tap(response => {
          // Cache the response for offline use
          this.cacheWeatherData(city, response);
          this.loadingSubject.next(false);
        }),
        catchError((error: HttpErrorResponse) => {
          this.loadingSubject.next(false);
          
          // If API fails, try to get cached data
          if (error.status === 0 || error.status >= 500) {
            const cachedData = this.getCachedWeatherData(city);
            if (cachedData) {
              return of(cachedData);
            }
          }
          
          return this.handleError(error);
        })
      );
  }

  getOfflineWeatherData(city: string): Observable<WeatherResponse> {
    this.loadingSubject.next(true);
    
    // First try to get from local cache
    const cachedData = this.getCachedWeatherData(city);
    if (cachedData) {
      this.loadingSubject.next(false);
      return of(cachedData);
    }

    // If no cache, try to get from backend's offline endpoint
    return this.http.get<WeatherResponse>(`${this.baseUrl}/offline/${city}`)
      .pipe(
        tap(response => {
          this.cacheWeatherData(city, response);
          this.loadingSubject.next(false);
        }),
        catchError((error: HttpErrorResponse) => {
          this.loadingSubject.next(false);
          return this.handleError(error);
        })
      );
  }

  private cacheWeatherData(city: string, data: WeatherResponse): void {
    try {
      const cache = this.getAllCachedData();
      cache[city.toLowerCase()] = {
        data,
        timestamp: Date.now()
      };
      localStorage.setItem(this.storageKey, JSON.stringify(cache));
    } catch (error) {
      console.warn('Failed to cache weather data:', error);
    }
  }

  private getCachedWeatherData(city: string): WeatherResponse | null {
    try {
      const cache = this.getAllCachedData();
      const cityData = cache[city.toLowerCase()];
      
      if (cityData && this.isCacheValid(cityData.timestamp)) {
        // Mark as offline mode in the cached data
        return {
          ...cityData.data,
          offline_mode: true
        };
      }
    } catch (error) {
      console.warn('Failed to get cached weather data:', error);
    }
    
    return null;
  }

  private getAllCachedData(): any {
    try {
      const cached = localStorage.getItem(this.storageKey);
      return cached ? JSON.parse(cached) : {};
    } catch {
      return {};
    }
  }

  private isCacheValid(timestamp: number): boolean {
    const cacheExpiryTime = 30 * 60 * 1000; // 30 minutes
    return Date.now() - timestamp < cacheExpiryTime;
  }

  clearCache(): void {
    localStorage.removeItem(this.storageKey);
  }

  getCachedCities(): string[] {
    try {
      const cache = this.getAllCachedData();
      return Object.keys(cache).map(city => 
        city.charAt(0).toUpperCase() + city.slice(1)
      );
    } catch {
      return [];
    }
  }

  private handleError(error: HttpErrorResponse): Observable<never> {
    let errorMessage = 'An unexpected error occurred';
    
    if (error.error instanceof ErrorEvent) {
      // Client-side error
      errorMessage = `Error: ${error.error.message}`;
    } else {
      // Server-side error
      if (error.error && error.error.message) {
        errorMessage = error.error.message;
      } else {
        switch (error.status) {
          case 0:
            errorMessage = 'Unable to connect to server. Please check your internet connection.';
            break;
          case 404:
            errorMessage = 'Weather data not found for this city.';
            break;
          case 500:
            errorMessage = 'Server error. Please try again later.';
            break;
          default:
            errorMessage = `Error: ${error.status} - ${error.message}`;
        }
      }
    }
    
    console.error('Weather Service Error:', error);
    return throwError(() => new Error(errorMessage));
  }
} 