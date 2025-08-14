import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Observable, BehaviorSubject, of, throwError } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap, catchError, map, tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { City, CitySearchResponse, CitySearchError } from '../models/city.model';

@Injectable({
  providedIn: 'root'
})
export class CityService {
  private readonly API_URL = `${environment.apiUrl}/cities`;
  private readonly CACHE_KEY = 'weather-app-city-cache';
  private readonly CACHE_DURATION = 60 * 60 * 1000; // 1 hour in milliseconds
  
  private searchResultsSubject = new BehaviorSubject<City[]>([]);
  private loadingSubject = new BehaviorSubject<boolean>(false);
  private errorSubject = new BehaviorSubject<string | null>(null);
  
  public searchResults$ = this.searchResultsSubject.asObservable();
  public loading$ = this.loadingSubject.asObservable();
  public error$ = this.errorSubject.asObservable();

  constructor(private http: HttpClient) {
    this.loadPopularCities();
  }

  /**
   * Search cities with debounced input
   */
  searchCities(query: string): Observable<City[]> {
    this.loadingSubject.next(true);
    this.errorSubject.next(null);
    
    if (!query || query.trim().length < 2) {
      return this.getPopularCities().pipe(
        tap(cities => {
          this.searchResultsSubject.next(cities);
          this.loadingSubject.next(false);
        }),
        catchError(error => this.handleError(error))
      );
    }

    const params = new HttpParams().set('q', query.trim());
    
    return this.http.get<CitySearchResponse>(`${this.API_URL}/search`, { params }).pipe(
      map(response => {
        this.cacheSearchResult(query, response);
        return response.cities;
      }),
      tap(cities => {
        this.searchResultsSubject.next(cities);
        this.loadingSubject.next(false);
      }),
      catchError(error => {
        this.loadingSubject.next(false);
        
        // Try to get cached results on error
        const cached = this.getCachedSearchResult(query);
        if (cached && cached.length > 0) {
          this.searchResultsSubject.next(cached);
          this.errorSubject.next('Using cached results - search service temporarily unavailable');
          return of(cached);
        }
        
        return this.handleError(error);
      })
    );
  }

  /**
   * Create debounced search observable
   */
  createDebouncedSearch(searchInput$: Observable<string>): Observable<City[]> {
    return searchInput$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(query => this.searchCities(query))
    );
  }

  /**
   * Get popular cities
   */
  getPopularCities(): Observable<City[]> {
    const cached = this.getCachedPopularCities();
    if (cached && cached.length > 0) {
      return of(cached);
    }

    return this.http.get<CitySearchResponse>(`${this.API_URL}/popular`).pipe(
      map(response => {
        this.cachePopularCities(response.cities);
        return response.cities;
      }),
      catchError(error => {
        console.warn('Failed to fetch popular cities:', error);
        return of(this.getFallbackCities());
      })
    );
  }

  /**
   * Load initial popular cities
   */
  private loadPopularCities(): void {
    this.getPopularCities().subscribe(
      cities => this.searchResultsSubject.next(cities),
      error => {
        console.error('Failed to load popular cities:', error);
        this.searchResultsSubject.next(this.getFallbackCities());
      }
    );
  }

  /**
   * Cache search result
   */
  private cacheSearchResult(query: string, response: CitySearchResponse): void {
    try {
      const cache = this.getCache();
      cache.searches = cache.searches || {};
      cache.searches[query.toLowerCase()] = {
        cities: response.cities,
        timestamp: Date.now(),
        type: response.type
      };
      localStorage.setItem(this.CACHE_KEY, JSON.stringify(cache));
    } catch (error) {
      console.warn('Failed to cache search result:', error);
    }
  }

  /**
   * Get cached search result
   */
  private getCachedSearchResult(query: string): City[] | null {
    try {
      const cache = this.getCache();
      const result = cache.searches?.[query.toLowerCase()];
      
      if (result && (Date.now() - result.timestamp) < this.CACHE_DURATION) {
        return result.cities;
      }
    } catch (error) {
      console.warn('Failed to get cached search result:', error);
    }
    return null;
  }

  /**
   * Cache popular cities
   */
  private cachePopularCities(cities: City[]): void {
    try {
      const cache = this.getCache();
      cache.popularCities = {
        cities,
        timestamp: Date.now()
      };
      localStorage.setItem(this.CACHE_KEY, JSON.stringify(cache));
    } catch (error) {
      console.warn('Failed to cache popular cities:', error);
    }
  }

  /**
   * Get cached popular cities
   */
  private getCachedPopularCities(): City[] | null {
    try {
      const cache = this.getCache();
      const popular = cache.popularCities;
      
      if (popular && (Date.now() - popular.timestamp) < this.CACHE_DURATION) {
        return popular.cities;
      }
    } catch (error) {
      console.warn('Failed to get cached popular cities:', error);
    }
    return null;
  }

  /**
   * Get cache object
   */
  private getCache(): any {
    try {
      const cached = localStorage.getItem(this.CACHE_KEY);
      return cached ? JSON.parse(cached) : {};
    } catch (error) {
      return {};
    }
  }

  /**
   * Fallback cities when all else fails
   */
  private getFallbackCities(): City[] {
    return [
      { name: 'London', country: 'United Kingdom', country_code: 'GB', state: 'England', latitude: 51.5074, longitude: -0.1278, display_name: 'London, England, United Kingdom' },
      { name: 'New York', country: 'United States', country_code: 'US', state: 'New York', latitude: 40.7128, longitude: -74.0060, display_name: 'New York, New York, United States' },
      { name: 'Tokyo', country: 'Japan', country_code: 'JP', state: 'Tokyo', latitude: 35.6762, longitude: 139.6503, display_name: 'Tokyo, Tokyo, Japan' },
      { name: 'Paris', country: 'France', country_code: 'FR', state: 'Île-de-France', latitude: 48.8566, longitude: 2.3522, display_name: 'Paris, Île-de-France, France' },
      { name: 'Berlin', country: 'Germany', country_code: 'DE', state: 'Berlin', latitude: 52.5200, longitude: 13.4050, display_name: 'Berlin, Berlin, Germany' }
    ];
  }

  /**
   * Handle HTTP errors
   */
  private handleError(error: HttpErrorResponse): Observable<never> {
    let errorMessage = 'An error occurred while searching cities';
    
    if (error.error instanceof ErrorEvent) {
      // Client-side error
      errorMessage = `Network error: ${error.error.message}`;
    } else {
      // Server-side error
      const serverError = error.error as CitySearchError;
      if (serverError && serverError.message) {
        errorMessage = serverError.message;
      } else {
        errorMessage = `Server error: ${error.status} ${error.statusText}`;
      }
    }
    
    this.errorSubject.next(errorMessage);
    this.searchResultsSubject.next(this.getFallbackCities());
    
    console.error('City search error:', error);
    return throwError(() => new Error(errorMessage));
  }

  /**
   * Clear cache
   */
  clearCache(): void {
    try {
      localStorage.removeItem(this.CACHE_KEY);
    } catch (error) {
      console.warn('Failed to clear city cache:', error);
    }
  }

  /**
   * Check service health
   */
  checkHealth(): Observable<any> {
    return this.http.get(`${this.API_URL}/health`);
  }
} 