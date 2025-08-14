import { test, expect } from '@playwright/test';

test.describe('Weather App E2E Tests', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test('should load the main page', async ({ page }) => {
    await expect(page).toHaveTitle(/Weather/);
    await expect(page.locator('h1')).toContainText('Weather');
  });

  test('should search for weather by city', async ({ page }) => {
    // Enter city name
    await page.fill('input[placeholder*="city"]', 'London');
    
    // Click search button or press Enter
    await page.press('input[placeholder*="city"]', 'Enter');
    
    // Wait for results
    await expect(page.locator('.weather-card')).toBeVisible({ timeout: 10000 });
    
    // Verify weather data is displayed
    await expect(page.locator('.weather-card')).toContainText('London');
    await expect(page.locator('.temperature')).toBeVisible();
  });

  test('should show city autocomplete suggestions', async ({ page }) => {
    // Start typing city name
    await page.fill('input[placeholder*="city"]', 'Lon');
    
    // Wait for suggestions to appear
    await expect(page.locator('.suggestions-dropdown')).toBeVisible({ timeout: 5000 });
    
    // Verify suggestions contain relevant cities
    await expect(page.locator('.suggestion-item')).toHaveCount.greaterThan(0);
    
    // Click on first suggestion
    await page.click('.suggestion-item:first-child');
    
    // Verify city is selected and search is triggered
    await expect(page.locator('.weather-card')).toBeVisible({ timeout: 10000 });
  });

  test('should toggle offline mode', async ({ page }) => {
    // Click offline mode toggle
    await page.click('[data-testid="offline-toggle"]');
    
    // Verify offline mode indicator is shown
    await expect(page.locator('.offline-indicator')).toBeVisible();
    
    // Try to search in offline mode
    await page.fill('input[placeholder*="city"]', 'London');
    await page.press('input[placeholder*="city"]', 'Enter');
    
    // Should show cached data or offline message
    await expect(page.locator('.offline-notice')).toBeVisible({ timeout: 5000 });
  });

  test('should display weather forecast cards', async ({ page }) => {
    // Search for a city
    await page.fill('input[placeholder*="city"]', 'London');
    await page.press('input[placeholder*="city"]', 'Enter');
    
    // Wait for forecast cards
    await expect(page.locator('.weather-card')).toBeVisible({ timeout: 10000 });
    
    // Verify forecast details
    await expect(page.locator('.high-temp')).toBeVisible();
    await expect(page.locator('.low-temp')).toBeVisible();
    await expect(page.locator('.weather-description')).toBeVisible();
    await expect(page.locator('.weather-icon')).toBeVisible();
  });

  test('should handle search errors gracefully', async ({ page }) => {
    // Search for invalid city
    await page.fill('input[placeholder*="city"]', 'InvalidCity12345');
    await page.press('input[placeholder*="city"]', 'Enter');
    
    // Should show error message or fallback
    await expect(page.locator('.error-message, .fallback-message')).toBeVisible({ timeout: 10000 });
  });

  test('should show loading states', async ({ page }) => {
    // Start search
    await page.fill('input[placeholder*="city"]', 'London');
    await page.press('input[placeholder*="city"]', 'Enter');
    
    // Should show loading indicator
    await expect(page.locator('.loading-spinner, .loading-indicator')).toBeVisible();
    
    // Loading should disappear when results load
    await expect(page.locator('.loading-spinner, .loading-indicator')).toBeHidden({ timeout: 10000 });
  });

  test('should display cached cities', async ({ page }) => {
    // Search for a city to add it to cache
    await page.fill('input[placeholder*="city"]', 'London');
    await page.press('input[placeholder*="city"]', 'Enter');
    await expect(page.locator('.weather-card')).toBeVisible({ timeout: 10000 });
    
    // Reload page
    await page.reload();
    
    // Should show cached cities section
    await expect(page.locator('.cached-cities')).toBeVisible();
    await expect(page.locator('.cached-city')).toContainText('London');
  });

  test('should be responsive on mobile', async ({ page }) => {
    // Set mobile viewport
    await page.setViewportSize({ width: 375, height: 667 });
    
    // Verify mobile layout
    await expect(page.locator('.mobile-menu, .responsive-layout')).toBeVisible();
    
    // Test search functionality on mobile
    await page.fill('input[placeholder*="city"]', 'London');
    await page.press('input[placeholder*="city"]', 'Enter');
    
    await expect(page.locator('.weather-card')).toBeVisible({ timeout: 10000 });
  });

  test('should show weather animations', async ({ page }) => {
    // Search for a city with specific weather
    await page.fill('input[placeholder*="city"]', 'London');
    await page.press('input[placeholder*="city"]', 'Enter');
    
    await expect(page.locator('.weather-card')).toBeVisible({ timeout: 10000 });
    
    // Check for weather animations based on conditions
    const weatherEffects = page.locator('.weather-effects, .weather-particles');
    await expect(weatherEffects).toBeVisible();
  });

  test('should handle keyboard navigation', async ({ page }) => {
    // Focus on search input
    await page.focus('input[placeholder*="city"]');
    
    // Type to trigger suggestions
    await page.type('input[placeholder*="city"]', 'Lon');
    
    // Wait for suggestions
    await expect(page.locator('.suggestions-dropdown')).toBeVisible({ timeout: 5000 });
    
    // Navigate with arrow keys
    await page.press('input[placeholder*="city"]', 'ArrowDown');
    await page.press('input[placeholder*="city"]', 'Enter');
    
    // Should select suggestion and search
    await expect(page.locator('.weather-card')).toBeVisible({ timeout: 10000 });
  });

  test('should persist user preferences', async ({ page }) => {
    // Toggle offline mode
    await page.click('[data-testid="offline-toggle"]');
    
    // Reload page
    await page.reload();
    
    // Offline mode should be remembered
    await expect(page.locator('.offline-indicator')).toBeVisible();
  });

  test('should show special weather conditions', async ({ page }) => {
    // Search for city that might have special conditions
    await page.fill('input[placeholder*="city"]', 'Phoenix');
    await page.press('input[placeholder*="city"]', 'Enter');
    
    await expect(page.locator('.weather-card')).toBeVisible({ timeout: 10000 });
    
    // Look for special condition alerts
    const specialConditions = page.locator('.special-condition, .weather-alert');
    if (await specialConditions.count() > 0) {
      await expect(specialConditions.first()).toBeVisible();
    }
  });

  test('should work with different weather conditions', async ({ page }) => {
    const cities = ['London', 'Dubai', 'Moscow', 'Mumbai'];
    
    for (const city of cities) {
      await page.fill('input[placeholder*="city"]', city);
      await page.press('input[placeholder*="city"]', 'Enter');
      
      await expect(page.locator('.weather-card')).toBeVisible({ timeout: 10000 });
      await expect(page.locator('.weather-card')).toContainText(city);
      
      // Clear search for next iteration
      await page.fill('input[placeholder*="city"]', '');
    }
  });
});

test.describe('API Integration Tests', () => {
  test('should handle API responses correctly', async ({ page }) => {
    // Intercept API calls
    await page.route('**/api/v1/weather/forecast*', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          city: 'London',
          forecasts: [{
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
          }],
          offline_mode: false,
          from_cache: false,
          total_days: 3,
          timestamp: '2024-01-15T12:00:00',
          notice: 'Live data from external API'
        })
      });
    });

    // Perform search
    await page.fill('input[placeholder*="city"]', 'London');
    await page.press('input[placeholder*="city"]', 'Enter');

    // Verify mocked response is displayed
    await expect(page.locator('.weather-card')).toContainText('London');
    await expect(page.locator('.weather-card')).toContainText('Partly cloudy');
  });

  test('should handle API errors', async ({ page }) => {
    // Intercept API calls with error
    await page.route('**/api/v1/weather/forecast*', async route => {
      await route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({
          error: 'Internal Server Error'
        })
      });
    });

    // Perform search
    await page.fill('input[placeholder*="city"]', 'London');
    await page.press('input[placeholder*="city"]', 'Enter');

    // Should show error handling
    await expect(page.locator('.error-message')).toBeVisible({ timeout: 10000 });
  });
});

test.describe('Performance Tests', () => {
  test('should load within acceptable time', async ({ page }) => {
    const startTime = Date.now();
    
    await page.goto('/');
    await expect(page.locator('h1')).toBeVisible();
    
    const loadTime = Date.now() - startTime;
    expect(loadTime).toBeLessThan(5000); // Should load within 5 seconds
  });

  test('should handle multiple rapid searches', async ({ page }) => {
    const cities = ['London', 'Paris', 'Tokyo', 'New York', 'Sydney'];
    
    for (const city of cities) {
      await page.fill('input[placeholder*="city"]', city);
      await page.press('input[placeholder*="city"]', 'Enter');
      
      // Don't wait for full response, just start next search
      await page.waitForTimeout(500);
    }
    
    // Final search should still work
    await page.fill('input[placeholder*="city"]', 'Berlin');
    await page.press('input[placeholder*="city"]', 'Enter');
    await expect(page.locator('.weather-card')).toBeVisible({ timeout: 10000 });
  });
}); 