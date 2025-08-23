-- D1 Database Schema for HuskerBot
-- Migration from JPA entities to SQLite

-- Schedule/Games table
CREATE TABLE schedules (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    season INTEGER NOT NULL,
    week INTEGER,
    date_time DATETIME NOT NULL,
    opponent TEXT NOT NULL,
    location TEXT,
    venue_type TEXT,
    tv_channel TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Betting table  
CREATE TABLE bets (
    user_id TEXT NOT NULL,
    game_id TEXT NOT NULL,
    bet_type TEXT NOT NULL,
    bet_amount INTEGER NOT NULL,
    bet_line REAL,
    potential_payout REAL,
    status TEXT DEFAULT 'active',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, game_id, bet_type)
);

-- Images table
CREATE TABLE images (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT UNIQUE NOT NULL,
    url TEXT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- NFL Games table (for pick'em)
CREATE TABLE nfl_games (
    id TEXT PRIMARY KEY,
    week INTEGER NOT NULL,
    season INTEGER NOT NULL,
    home_team TEXT NOT NULL,
    away_team TEXT NOT NULL,
    game_time DATETIME NOT NULL,
    home_score INTEGER,
    away_score INTEGER,
    status TEXT DEFAULT 'scheduled',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- NFL Picks table
CREATE TABLE nfl_picks (
    user_id TEXT NOT NULL,
    game_id TEXT NOT NULL,
    picked_team TEXT NOT NULL,
    confidence INTEGER,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, game_id),
    FOREIGN KEY (game_id) REFERENCES nfl_games(id)
);

-- Indexes for performance
CREATE INDEX idx_schedules_season_date ON schedules(season, date_time);
CREATE INDEX idx_bets_user_id ON bets(user_id);
CREATE INDEX idx_bets_game_id ON bets(game_id);  
CREATE INDEX idx_nfl_games_week_season ON nfl_games(week, season);
CREATE INDEX idx_nfl_picks_user_id ON nfl_picks(user_id);