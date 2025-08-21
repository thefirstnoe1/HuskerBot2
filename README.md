# HuskerBot2

[![Build Status](https://github.com/j3y/HuskerBot2/actions/workflows/gradle.yml/badge.svg)](https://github.com/j3y/HuskerBot2/actions/workflows/gradle.yml)

HuskerBot2 is a Kotlin/Spring Boot Discord bot for Nebraska Cornhuskers fans. It provides game-day utilities, betting games, pick'em, schedules, weather, and other fun commands.

## Features
- Betting commands
  - Create and show weekly bets for Husker games
  - View betting lines for a given week
  - Season-long betting leaderboard with points: Winner = 1, Spread = 2, Points = 2
- NFL Pick'em
  - Interactive buttons to pick winners; live counts update on the message
  - Leaderboard command for season performance
- Schedules
  - College Football (CFB) schedule and countdowns
  - NFL schedule utilities
- Weather
  - Game day weather via NWS (api.weather.gov)
- Other utilities
  - Urban Dictionary, image management, OSRS stats, and more

## Tech Stack
- Kotlin + Spring Boot
- JDA (Java Discord API)
- Spring Data JPA with H2 (file-backed) datastore
- Gradle build

## Requirements
- Java 17 or newer
- Discord bot token with appropriate permissions
- (Optional) API keys for external services (e.g., CollegeFootballData)

## Configuration
Edit `src/main/resources/application.yml`:
- `discord.token`: Your bot token
- `discord.guilds`: Guild IDs where the bot should operate
- Channels and category IDs as needed
- `cfbd.api-key`: CollegeFootballData API key if you use lines/matchup features
- Weather and Urban Dictionary settings are already configured with sensible defaults

## Run Locally
1. Set your configuration in `application.yml`.
2. Build and run using the Gradle wrapper:
   - macOS/Linux: `./gradlew bootRun`
   - Windows: `gradlew.bat bootRun`

To build a jar:
- macOS/Linux: `./gradlew build`
- Windows: `gradlew.bat build`

## Testing
- Unit tests: `./gradlew test`
- Additional testing scripts live under the `testing/` directory (Python and shell helpers for weather parsing).

## Commands (selected)
- `/bet create` — create your weekly bet (winner, spread, over/under)
- `/bet show` — list all bets for a chosen week and totals
- `/bet lines` — show the betting lines for the selected week
- `/bet leaderboard` — season-long points leaderboard
- `/schedule` commands — CFB and NFL schedule utilities
- `/gameday` commands — game-day on/off and weather
- `/image` commands — add/list/delete/show stored images
- Misc: `/urban`, `/compare`, `/osrs`, `/possum`, etc.

## Notes
- The bot uses a file-based H2 database located at `~/.discordbot/data` by default. Spring will auto-create/update schema on startup.
- Some features rely on external APIs and may need keys or rate limits respected.

## Contributing
PRs and issues welcome. Please keep changes small and focused. Run tests before submitting.
