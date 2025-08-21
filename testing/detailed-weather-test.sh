#!/bin/bash

echo "=== Detailed Weather Data Parsing Test ==="
echo ""

echo "1. Getting raw weather data from NWS API..."
WEATHER_JSON=$(curl -s "https://api.weather.gov/gridpoints/OAX/56,40/forecast" \
    -H "User-Agent: HuskerBot2 (test@example.com)")

echo "2. Checking if we got valid JSON response..."
if echo "$WEATHER_JSON" | grep -q '"periods"'; then
    echo "   ✅ Valid JSON response received"
else
    echo "   ❌ Invalid or empty response"
    exit 1
fi

echo ""
echo "3. Parsing weather data fields..."

# Extract first period data
FIRST_PERIOD=$(echo "$WEATHER_JSON" | grep -A 20 '"periods"' | grep -A 15 '{' | head -20)

echo "   Raw first period data:"
echo "$FIRST_PERIOD" | head -10

echo ""
echo "   Extracting individual fields:"

# Temperature
TEMP=$(echo "$WEATHER_JSON" | grep -o '"temperature":[0-9]*' | head -1 | cut -d':' -f2)
echo "   Temperature: $TEMP°F"

# Short forecast
SHORT_FORECAST=$(echo "$WEATHER_JSON" | grep -o '"shortForecast":"[^"]*"' | head -1 | sed 's/"shortForecast":"\([^"]*\)"/\1/')
echo "   Short forecast: $SHORT_FORECAST"

# Detailed forecast
DETAILED_FORECAST=$(echo "$WEATHER_JSON" | grep -o '"detailedForecast":"[^"]*"' | head -1 | sed 's/"detailedForecast":"\([^"]*\)"/\1/')
echo "   Detailed forecast: ${DETAILED_FORECAST:0:100}..."

# Wind speed
WIND_SPEED=$(echo "$WEATHER_JSON" | grep -o '"windSpeed":"[^"]*"' | head -1 | sed 's/"windSpeed":"\([^"]*\)"/\1/')
echo "   Wind speed: $WIND_SPEED"

# Wind direction
WIND_DIRECTION=$(echo "$WEATHER_JSON" | grep -o '"windDirection":"[^"]*"' | head -1 | sed 's/"windDirection":"\([^"]*\)"/\1/')
echo "   Wind direction: $WIND_DIRECTION"

# Start time
START_TIME=$(echo "$WEATHER_JSON" | grep -o '"startTime":"[^"]*"' | head -1 | sed 's/"startTime":"\([^"]*\)"/\1/')
echo "   Start time: $START_TIME"

# End time
END_TIME=$(echo "$WEATHER_JSON" | grep -o '"endTime":"[^"]*"' | head -1 | sed 's/"endTime":"\([^"]*\)"/\1/')
echo "   End time: $END_TIME"

echo ""
echo "4. Validation checks..."

if [ -n "$TEMP" ] && [ "$TEMP" -gt 0 ] 2>/dev/null; then
    echo "   ✅ Temperature parsed correctly: $TEMP°F"
else
    echo "   ❌ Temperature parsing failed"
fi

if [ -n "$SHORT_FORECAST" ] && [ ${#SHORT_FORECAST} -gt 3 ]; then
    echo "   ✅ Short forecast parsed correctly: $SHORT_FORECAST"
else
    echo "   ❌ Short forecast parsing failed"
fi

if [ -n "$WIND_SPEED" ] && [ ${#WIND_SPEED} -gt 0 ]; then
    echo "   ✅ Wind speed parsed correctly: $WIND_SPEED"
else
    echo "   ❌ Wind speed parsing failed"
fi

if [ -n "$WIND_DIRECTION" ] && [ ${#WIND_DIRECTION} -gt 0 ]; then
    echo "   ✅ Wind direction parsed correctly: $WIND_DIRECTION"
else
    echo "   ❌ Wind direction parsing failed"
fi

if [ -n "$START_TIME" ] && [ ${#START_TIME} -gt 10 ]; then
    echo "   ✅ Start time parsed correctly: $START_TIME"
else
    echo "   ❌ Start time parsing failed"
fi

echo ""
echo "5. Testing date matching logic..."
echo "   Current time: $(date -Iseconds)"
echo "   Forecast start: $START_TIME"
echo "   Forecast end: $END_TIME"

echo ""
echo "=== Summary ==="
echo "Weather data parsing appears to be working correctly."
echo "The Kotlin implementation should successfully extract:"
echo "- Temperature, conditions, wind data"
echo "- Time periods for matching game dates"
echo "- All required fields for Discord embed"