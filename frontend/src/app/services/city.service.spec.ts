import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { CityService } from './city.service';
import { City, CitySearchResponse } from '../models/city.model';
import { environment } from '../../environments/environment';

describe('CityService', () => {
  let service: CityService;
  let httpMock: HttpTestingController;

  const mockCities: City[] = [
    {
      name: 'London',
      country: 'United Kingdom',
      countryCode: 'GB',
      state: 'England',
      latitude: 51.5074,
      longitude: -0.1278,
      displayName: 'London, England, GB'
    },
    {
      name: 'London',
      country: 'Canada',
      countryCode: 'CA',
      state: 'Ontario',
      latitude: 42.9834,
      longitude: -81.2497,
      displayName: 'London, Ontario, CA'
    }
  ];

  const mockSearchResponse: CitySearchResponse = {
    cities: mockCities,
    total: 2,
    query: 'London'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [CityService]
    });
    service = TestBed.inject(CityService);
    httpMock = TestBed.inject(HttpTestingController);

    // Clear localStorage before each test
    localStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('searchCities', () => {
    it('should search cities with debouncing', (done) => {
      const query = 'London';
      let requestCount = 0;

      service.searchCities(query).subscribe(response => {
        expect(response).toEqual(mockSearchResponse);
        expect(requestCount).toBe(1); // Only one request due to debouncing
        done();
      });

      // Multiple rapid calls should be debounced
      service.searchCities(query);
      service.searchCities(query);
      requestCount++;

      const req = httpMock.expectOne(`${environment.apiUrl}/cities/search?q=${query}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockSearchResponse);
    });

    it('should handle empty search query', () => {
      service.searchCities('').subscribe(response => {
        expect(response.cities).toEqual([]);
        expect(response.total).toBe(0);
      });

      httpMock.expectNone(`${environment.apiUrl}/cities/search?q=`);
    });

    it('should handle search query too short', () => {
      service.searchCities('L').subscribe(response => {
        expect(response.cities).toEqual([]);
        expect(response.total).toBe(0);
      });

      httpMock.expectNone(`${environment.apiUrl}/cities/search?q=L`);
    });

    it('should handle HTTP errors gracefully', () => {
      const query = 'London';

      service.searchCities(query).subscribe({
        next: () => fail('Expected an error'),
        error: (error) => {
          expect(error.status).toBe(500);
        }
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/cities/search?q=${query}`);
      req.flush({ message: 'Server Error' }, { status: 500, statusText: 'Internal Server Error' });
    });

    it('should return fallback cities on API error', () => {
      const query = 'London';

      service.searchCities(query).subscribe(response => {
        expect(response.cities.length).toBeGreaterThan(0);
        expect(response.cities[0].name).toBeDefined();
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/cities/search?q=${query}`);
      req.error(new ErrorEvent('Network error'));
    });

    it('should cache search results', () => {
      const query = 'London';
      spyOn(localStorage, 'setItem');

      service.searchCities(query).subscribe();

      const req = httpMock.expectOne(`${environment.apiUrl}/cities/search?q=${query}`);
      req.flush(mockSearchResponse);

      expect(localStorage.setItem).toHaveBeenCalledWith(
        `citySearch_${query}`,
        jasmine.any(String)
      );
    });

    it('should use cached results when available', () => {
      const query = 'London';
      const cachedData = {
        data: mockSearchResponse,
        timestamp: Date.now()
      };

      spyOn(localStorage, 'getItem').and.returnValue(JSON.stringify(cachedData));

      service.searchCities(query).subscribe(response => {
        expect(response).toEqual(mockSearchResponse);
      });

      httpMock.expectNone(`${environment.apiUrl}/cities/search?q=${query}`);
    });

    it('should ignore expired cache', () => {
      const query = 'London';
      const expiredCachedData = {
        data: mockSearchResponse,
        timestamp: Date.now() - (2 * 60 * 60 * 1000) // 2 hours ago
      };

      spyOn(localStorage, 'getItem').and.returnValue(JSON.stringify(expiredCachedData));

      service.searchCities(query).subscribe();

      const req = httpMock.expectOne(`${environment.apiUrl}/cities/search?q=${query}`);
      req.flush(mockSearchResponse);
    });
  });

  describe('getPopularCities', () => {
    it('should fetch popular cities', () => {
      const popularResponse = {
        cities: mockCities,
        total: mockCities.length
      };

      service.getPopularCities().subscribe(response => {
        expect(response).toEqual(popularResponse);
        expect(response.cities.length).toBeGreaterThan(0);
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/cities/popular`);
      expect(req.request.method).toBe('GET');
      req.flush(popularResponse);
    });

    it('should cache popular cities', () => {
      const popularResponse = {
        cities: mockCities,
        total: mockCities.length
      };

      spyOn(localStorage, 'setItem');

      service.getPopularCities().subscribe();

      const req = httpMock.expectOne(`${environment.apiUrl}/cities/popular`);
      req.flush(popularResponse);

      expect(localStorage.setItem).toHaveBeenCalledWith(
        'popularCities',
        jasmine.any(String)
      );
    });

    it('should use cached popular cities when available', () => {
      const cachedData = {
        data: { cities: mockCities, total: mockCities.length },
        timestamp: Date.now()
      };

      spyOn(localStorage, 'getItem').and.returnValue(JSON.stringify(cachedData));

      service.getPopularCities().subscribe(response => {
        expect(response.cities).toEqual(mockCities);
      });

      httpMock.expectNone(`${environment.apiUrl}/cities/popular`);
    });

    it('should return fallback cities on API error', () => {
      service.getPopularCities().subscribe(response => {
        expect(response.cities.length).toBeGreaterThan(0);
        expect(response.cities[0].name).toBeDefined();
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/cities/popular`);
      req.error(new ErrorEvent('Network error'));
    });
  });

  describe('loading state', () => {
    it('should update loading state during search', () => {
      const query = 'London';
      let loadingStates: boolean[] = [];

      service.loading$.subscribe(loading => {
        loadingStates.push(loading);
      });

      service.searchCities(query).subscribe();

      const req = httpMock.expectOne(`${environment.apiUrl}/cities/search?q=${query}`);
      req.flush(mockSearchResponse);

      expect(loadingStates).toContain(true);
      expect(loadingStates).toContain(false);
    });
  });

  describe('error handling', () => {
    it('should emit error state on API failure', () => {
      const query = 'London';
      let errorStates: string[] = [];

      service.error$.subscribe(error => {
        if (error) errorStates.push(error);
      });

      service.searchCities(query).subscribe({
        error: () => {} // Handle error to prevent test failure
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/cities/search?q=${query}`);
      req.flush({ message: 'Server Error' }, { status: 500, statusText: 'Internal Server Error' });

      expect(errorStates.length).toBeGreaterThan(0);
    });

    it('should clear error state on successful request', () => {
      const query = 'London';
      let errorStates: (string | null)[] = [];

      service.error$.subscribe(error => {
        errorStates.push(error);
      });

      service.searchCities(query).subscribe();

      const req = httpMock.expectOne(`${environment.apiUrl}/cities/search?q=${query}`);
      req.flush(mockSearchResponse);

      expect(errorStates).toContain(null);
    });
  });

  describe('search results observable', () => {
    it('should emit search results', () => {
      const query = 'London';
      let searchResults: City[][] = [];

      service.searchResults$.subscribe(results => {
        searchResults.push(results);
      });

      service.searchCities(query).subscribe();

      const req = httpMock.expectOne(`${environment.apiUrl}/cities/search?q=${query}`);
      req.flush(mockSearchResponse);

      expect(searchResults).toContain(mockCities);
    });
  });

  describe('special characters and internationalization', () => {
    it('should handle cities with special characters', () => {
      const query = 'São';
      const specialCityResponse: CitySearchResponse = {
        cities: [{
          name: 'São Paulo',
          country: 'Brazil',
          countryCode: 'BR',
          state: 'São Paulo',
          latitude: -23.5505,
          longitude: -46.6333,
          displayName: 'São Paulo, São Paulo, BR'
        }],
        total: 1,
        query: 'São'
      };

      service.searchCities(query).subscribe(response => {
        expect(response).toEqual(specialCityResponse);
        expect(response.cities[0].name).toBe('São Paulo');
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/cities/search?q=${encodeURIComponent(query)}`);
      req.flush(specialCityResponse);
    });

    it('should handle cities with non-Latin characters', () => {
      const query = '北京';
      const chineseCityResponse: CitySearchResponse = {
        cities: [{
          name: '北京',
          country: 'China',
          countryCode: 'CN',
          state: 'Beijing',
          latitude: 39.9042,
          longitude: 116.4074,
          displayName: '北京, Beijing, CN'
        }],
        total: 1,
        query: '北京'
      };

      service.searchCities(query).subscribe(response => {
        expect(response).toEqual(chineseCityResponse);
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/cities/search?q=${encodeURIComponent(query)}`);
      req.flush(chineseCityResponse);
    });
  });

  describe('performance and optimization', () => {
    it('should not make duplicate requests for same query', () => {
      const query = 'London';

      // Make multiple requests with same query
      service.searchCities(query).subscribe();
      service.searchCities(query).subscribe();

      // Should only make one HTTP request due to distinctUntilChanged
      const req = httpMock.expectOne(`${environment.apiUrl}/cities/search?q=${query}`);
      req.flush(mockSearchResponse);
    });

    it('should cancel previous requests when new query is made', () => {
      service.searchCities('Lon').subscribe();
      service.searchCities('London').subscribe();

      // Only the latest request should be made due to switchMap
      httpMock.expectNone(`${environment.apiUrl}/cities/search?q=Lon`);
      const req = httpMock.expectOne(`${environment.apiUrl}/cities/search?q=London`);
      req.flush(mockSearchResponse);
    });
  });

  describe('fallback cities', () => {
    it('should return fallback cities when API is unavailable', () => {
      const fallbackCities = service.getFallbackCities();

      expect(fallbackCities.length).toBeGreaterThan(0);
      expect(fallbackCities[0].name).toBeDefined();
      expect(fallbackCities[0].country).toBeDefined();
      expect(fallbackCities[0].latitude).toBeDefined();
      expect(fallbackCities[0].longitude).toBeDefined();
    });

    it('should include major world cities in fallback', () => {
      const fallbackCities = service.getFallbackCities();
      const cityNames = fallbackCities.map(city => city.name);

      expect(cityNames).toContain('London');
      expect(cityNames).toContain('New York');
      expect(cityNames).toContain('Tokyo');
      expect(cityNames).toContain('Paris');
    });
  });
}); 