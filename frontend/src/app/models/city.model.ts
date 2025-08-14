export interface City {
  name: string;
  country: string;
  country_code: string;
  state?: string;
  latitude: number;
  longitude: number;
  display_name: string;
}

export interface CitySearchResponse {
  query: string;
  cities: City[];
  count: number;
  type: 'popular_cities' | 'fallback_search' | 'live_search';
  message: string;
  timestamp: string;
}

export interface CitySearchError {
  error: boolean;
  message: string;
  statusCode: number;
  timestamp: string;
  service: string;
} 