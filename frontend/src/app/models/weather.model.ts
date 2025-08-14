export interface WeatherForecast {
  city: string;
  forecastDate: string;
  high_temp: number;
  low_temp: number;
  description: string;
  weather_condition: string;
  wind_speed: number;
  humidity: number;
  pressure: number;
  special_condition: string;
}

export interface WeatherResponse {
  city: string;
  forecasts: WeatherForecast[];
  offline_mode: boolean;
  from_cache: boolean;
  total_days: number;
  timestamp: string;
  notice?: string;
}

export interface WeatherError {
  error: boolean;
  message: string;
  timestamp: number;
} 