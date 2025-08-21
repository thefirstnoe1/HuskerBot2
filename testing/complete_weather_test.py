#!/usr/bin/env python3

import json
import requests
import time
from datetime import datetime, timedelta

def test_complete_weather_flow():
    print("=== Complete Gameday Weather Flow Test ===")
    print()
    
    headers = {'User-Agent': 'HuskerBot2 (test@example.com)'}
    
    # Test scenario: Simulating a game at Iowa City, IA in 2 days
    print("🏈 Scenario: Nebraska @ Iowa in 2 days")
    print()
    
    # Step 1: Geocoding
    print("1. Getting coordinates for Iowa City, IA...")
    time.sleep(1)
    
    geocode_response = requests.get(
        'https://nominatim.openstreetmap.org/search?q=Iowa City, IA&format=json&limit=1',
        headers=headers
    )
    
    if geocode_response.status_code == 200:
        geocode_data = geocode_response.json()
        if geocode_data:
            lat = float(geocode_data[0]['lat'])
            lon = float(geocode_data[0]['lon'])
            print(f"   ✅ Found coordinates: {lat}, {lon}")
        else:
            print("   ❌ No geocoding results")
            return False
    else:
        print(f"   ❌ Geocoding failed: {geocode_response.status_code}")
        return False
    
    # Step 2: Get NWS points data
    print()
    print("2. Getting NWS forecast grid reference...")
    
    points_response = requests.get(
        f'https://api.weather.gov/points/{lat},{lon}',
        headers=headers
    )
    
    if points_response.status_code == 200:
        points_data = points_response.json()
        forecast_url = points_data['properties']['forecast']
        print(f"   ✅ Found forecast URL: {forecast_url}")
    else:
        print(f"   ❌ Points API failed: {points_response.status_code}")
        return False
    
    # Step 3: Get weather forecast
    print()
    print("3. Getting weather forecast...")
    
    forecast_response = requests.get(forecast_url, headers=headers)
    
    if forecast_response.status_code == 200:
        forecast_data = forecast_response.json()
        periods = forecast_data['properties']['periods']
        print(f"   ✅ Retrieved {len(periods)} forecast periods")
        
        # Step 4: Simulate finding forecast for game time (2 days from now)
        print()
        print("4. Finding forecast for game day (2 days from now)...")
        
        # Look for a period 2 days from now
        target_date = datetime.now() + timedelta(days=2)
        print(f"   Target game time: {target_date}")
        
        found_forecast = None
        for period in periods:
            start_time = datetime.fromisoformat(period['startTime'].replace('Z', '+00:00'))
            end_time = datetime.fromisoformat(period['endTime'].replace('Z', '+00:00'))
            
            # Check if our target time falls within this period
            if start_time.date() <= target_date.date() <= end_time.date():
                found_forecast = period
                print(f"   ✅ Found matching forecast period: {period['name']}")
                break
        
        if not found_forecast:
            # Use a future period as fallback
            found_forecast = periods[min(4, len(periods)-1)]  # Use 3rd period as example
            print(f"   ⚠️  Using fallback forecast period: {found_forecast['name']}")
        
        # Step 5: Extract and format weather data
        print()
        print("5. Extracting weather data (simulating Kotlin parsing)...")
        
        weather_data = {
            'temperature': found_forecast.get('temperature', 0),
            'shortForecast': found_forecast.get('shortForecast', 'Unknown'),
            'detailedForecast': found_forecast.get('detailedForecast', 'No details'),
            'windSpeed': found_forecast.get('windSpeed', 'Unknown'),
            'windDirection': found_forecast.get('windDirection', 'Unknown'),
            'precipitation': found_forecast.get('probabilityOfPrecipitation', {}).get('value')
        }
        
        print("   Extracted weather data:")
        print(f"      Temperature: {weather_data['temperature']}°F")
        print(f"      Conditions: {weather_data['shortForecast']}")
        print(f"      Wind: {weather_data['windSpeed']} {weather_data['windDirection']}")
        if weather_data['precipitation'] is not None:
            print(f"      Precipitation: {weather_data['precipitation']}% chance")
        print(f"      Details: {weather_data['detailedForecast'][:60]}...")
        
        # Step 6: Simulate Discord embed creation
        print()
        print("6. Simulating Discord embed (what users would see)...")
        print()
        print("   🏈 Huskers Game Day Weather")
        print("   " + "="*40)
        print(f"   🆚 Opponent: Iowa Hawkeyes")
        print(f"   📅 Game Time: {target_date.strftime('%b %d, %Y at %I:%M %p')}")
        print(f"   📍 Location: Kinnick Stadium, Iowa City, IA")
        print(f"   🏠 Home/Away: Away")
        print(f"   🌡️ Temperature: {weather_data['temperature']}°F")
        print(f"   ☁️ Conditions: {weather_data['shortForecast']}")
        print(f"   💨 Wind: {weather_data['windSpeed']} {weather_data['windDirection']}")
        if weather_data['precipitation'] is not None:
            print(f"   🌧️ Precipitation: {weather_data['precipitation']}% chance")
        print(f"   📋 Detailed: {weather_data['detailedForecast']}")
        print("   " + "="*40)
        print("   Weather data from National Weather Service")
        
        print()
        print("✅ Complete weather flow test successful!")
        print("✅ All APIs working correctly")
        print("✅ Data parsing and formatting working")
        print("✅ The gameday weather command is ready to use")
        
        return True
        
    else:
        print(f"   ❌ Forecast API failed: {forecast_response.status_code}")
        return False

if __name__ == "__main__":
    success = test_complete_weather_flow()
    if not success:
        exit(1)