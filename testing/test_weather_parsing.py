#!/usr/bin/env python3

import json
import requests
import sys
from datetime import datetime

def test_weather_parsing():
    print("=== Weather Data Parsing Validation ===")
    print()
    
    print("1. Fetching weather data from NWS API...")
    
    headers = {
        'User-Agent': 'HuskerBot2 (test@example.com)'
    }
    
    try:
        # Get weather forecast for Lincoln, NE
        response = requests.get(
            'https://api.weather.gov/gridpoints/OAX/56,40/forecast',
            headers=headers
        )
        response.raise_for_status()
        
        weather_data = response.json()
        print("   ✅ Successfully fetched weather data")
        
    except Exception as e:
        print(f"   ❌ Failed to fetch weather data: {e}")
        return False
    
    print()
    print("2. Parsing weather periods...")
    
    try:
        periods = weather_data['properties']['periods']
        print(f"   ✅ Found {len(periods)} forecast periods")
        
        if len(periods) == 0:
            print("   ❌ No forecast periods available")
            return False
            
    except KeyError:
        print("   ❌ Invalid JSON structure - no periods found")
        return False
    
    print()
    print("3. Extracting data from first period...")
    
    first_period = periods[0]
    
    # Extract all the fields our Kotlin code expects
    fields = {
        'temperature': first_period.get('temperature'),
        'shortForecast': first_period.get('shortForecast'),
        'detailedForecast': first_period.get('detailedForecast'),
        'windSpeed': first_period.get('windSpeed'),
        'windDirection': first_period.get('windDirection'),
        'startTime': first_period.get('startTime'),
        'endTime': first_period.get('endTime'),
        'name': first_period.get('name')
    }
    
    print("   Extracted fields:")
    for field, value in fields.items():
        print(f"      {field}: {value}")
    
    print()
    print("4. Validating field extraction...")
    
    success = True
    
    # Validate temperature
    if isinstance(fields['temperature'], int) and fields['temperature'] > 0:
        print(f"   ✅ Temperature: {fields['temperature']}°F")
    else:
        print(f"   ❌ Temperature validation failed: {fields['temperature']}")
        success = False
    
    # Validate short forecast
    if fields['shortForecast'] and len(fields['shortForecast']) > 0:
        print(f"   ✅ Short forecast: {fields['shortForecast']}")
    else:
        print(f"   ❌ Short forecast validation failed: {fields['shortForecast']}")
        success = False
    
    # Validate detailed forecast
    if fields['detailedForecast'] and len(fields['detailedForecast']) > 10:
        print(f"   ✅ Detailed forecast: {fields['detailedForecast'][:50]}...")
    else:
        print(f"   ❌ Detailed forecast validation failed")
        success = False
    
    # Validate wind data
    if fields['windSpeed'] and fields['windDirection']:
        print(f"   ✅ Wind: {fields['windSpeed']} {fields['windDirection']}")
    else:
        print(f"   ❌ Wind data validation failed")
        success = False
    
    # Validate time data
    if fields['startTime'] and fields['endTime']:
        print(f"   ✅ Time period: {fields['startTime']} to {fields['endTime']}")
    else:
        print(f"   ❌ Time period validation failed")
        success = False
    
    print()
    print("5. Testing date parsing (simulating Kotlin logic)...")
    
    try:
        # Parse the ISO datetime strings
        start_time = datetime.fromisoformat(fields['startTime'].replace('Z', '+00:00'))
        end_time = datetime.fromisoformat(fields['endTime'].replace('Z', '+00:00'))
        
        print(f"   ✅ Start time parsed: {start_time}")
        print(f"   ✅ End time parsed: {end_time}")
        
        # Test if current time would fall within this period
        now = datetime.now(start_time.tzinfo)
        if start_time <= now <= end_time:
            print(f"   ✅ Current time ({now}) falls within this forecast period")
        else:
            print(f"   ℹ️  Current time ({now}) is outside this forecast period")
            
    except Exception as e:
        print(f"   ❌ Date parsing failed: {e}")
        success = False
    
    print()
    print("6. Testing multiple periods for date matching...")
    
    try:
        # Show all available periods
        print("   Available forecast periods:")
        for i, period in enumerate(periods[:5]):  # Show first 5
            start = period.get('startTime', 'N/A')
            name = period.get('name', 'N/A')
            temp = period.get('temperature', 'N/A')
            conditions = period.get('shortForecast', 'N/A')
            print(f"      {i+1}. {name}: {temp}°F, {conditions} (starts: {start})")
            
    except Exception as e:
        print(f"   ❌ Failed to process multiple periods: {e}")
        success = False
    
    print()
    print("=== Test Summary ===")
    
    if success:
        print("✅ All weather data parsing tests passed!")
        print("✅ The Kotlin WeatherService implementation should work correctly")
        print("✅ JSON structure matches expected format")
        print("✅ All required fields are available and parseable")
    else:
        print("❌ Some weather data parsing tests failed")
        print("⚠️  The Kotlin implementation may need adjustments")
    
    return success

if __name__ == "__main__":
    success = test_weather_parsing()
    sys.exit(0 if success else 1)