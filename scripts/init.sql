-- Database weatherdb will be created by docker-compose environment

-- Create user if not exists
DO
$do$
BEGIN
   IF NOT EXISTS (
      SELECT FROM pg_catalog.pg_roles
      WHERE  rolname = 'weatheruser') THEN

      CREATE ROLE weatheruser LOGIN PASSWORD 'weatherpass';
   END IF;
END
$do$;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE weatherdb TO weatheruser;

-- Connect to the database
\c weatherdb;

-- Grant schema privileges
GRANT ALL ON SCHEMA public TO weatheruser;

-- Create indexes for better performance (will be created by JPA but good to have)
-- These will be created automatically by Hibernate, but we can prepare them

-- Sample data for testing (optional)
-- INSERT INTO weather_forecasts (city, forecast_date, high_temp, low_temp, description, weather_condition, wind_speed, humidity, pressure, special_condition, created_at, updated_at)
-- VALUES 
--   ('London', CURRENT_DATE, 22.5, 15.3, 'Partly cloudy', 'Clouds', 5.2, 65, 1013.25, 'Have a great day!', NOW(), NOW()),
--   ('London', CURRENT_DATE + INTERVAL '1 day', 25.1, 17.8, 'Sunny', 'Clear', 3.8, 58, 1015.30, 'Use sunscreen lotion', NOW(), NOW()),
--   ('London', CURRENT_DATE + INTERVAL '2 days', 18.9, 12.4, 'Light rain', 'Rain', 8.5, 78, 1008.15, 'Carry umbrella', NOW(), NOW());