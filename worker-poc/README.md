# HuskerBot Cloudflare Worker POC

This is a proof-of-concept for running HuskerBot on Cloudflare Workers using Kotlin/WebAssembly.

## Architecture Overview

### Current (Spring Boot)
- **Runtime**: JVM with Spring Boot
- **Discord Connection**: WebSocket Gateway connection  
- **Database**: H2/MySQL with JPA/Hibernate
- **Architecture**: Event-driven listeners
- **Deployment**: Traditional server hosting

### Proposed (Cloudflare Workers)
- **Runtime**: V8 with Kotlin/WebAssembly
- **Discord Connection**: HTTP webhooks
- **Database**: Cloudflare D1 (SQLite) or external database
- **Architecture**: Request/response HTTP handlers
- **Deployment**: Serverless edge computing

## Key Benefits of Workers

1. **Global Edge Distribution**: Sub-50ms response times worldwide
2. **Zero Cold Starts**: V8 isolates start faster than containers
3. **Cost Efficiency**: Pay per request vs always-on servers
4. **Scalability**: Automatic scaling to millions of requests
5. **Developer Experience**: Simple deployment with Wrangler

## Technical Approach

### Kotlin/Wasm Compilation
- Uses Kotlin Multiplatform with `wasmJs` target
- Compiles Kotlin code to WebAssembly binary
- JavaScript wrapper handles Worker interface
- Access to all Cloudflare Worker APIs

### Discord Integration
**Webhook Architecture:**
```
Discord → Cloudflare Worker → Response
```
- Replace WebSocket Gateway with Discord Interactions API
- HTTP POST webhooks for slash commands
- Immediate responses or deferred replies
- Signature verification for security

### Database Migration Options

1. **Cloudflare D1 (Recommended)**
   - SQLite-compatible database
   - Built-in to Workers platform
   - Automatic replication and scaling
   - HTTP API for database operations

2. **External Database**
   - PlanetScale, Neon, or Supabase
   - Connection pooling required
   - Higher latency but more features

## Implementation Status

### ✅ Completed
- [x] Basic Kotlin/Wasm project structure
- [x] Discord webhook interaction handling
- [x] Proof-of-concept slash command responses
- [x] Worker deployment configuration

### 🚧 In Progress  
- [ ] Database integration (D1 setup)
- [ ] Weather service adaptation
- [ ] Authentication and security

### 📋 Planned
- [ ] Binary size optimization
- [ ] Performance testing
- [ ] Full feature migration
- [ ] Production deployment

## Quick Start

1. **Build Kotlin/Wasm:**
   ```bash
   cd worker-poc
   ../gradlew wasmJsBrowserDistribution
   ```

2. **Deploy to Cloudflare:**
   ```bash
   npm install
   wrangler dev    # Local development
   wrangler deploy # Production deployment
   ```

3. **Setup Discord Webhook:**
   - Configure Discord Application interactions endpoint
   - Point to Worker URL: `https://huskerbot-worker.your-subdomain.workers.dev`

## Database Schema Migration

Current JPA entities need conversion to D1-compatible SQL:

```sql
-- Example: BetEntity migration
CREATE TABLE bets (
    user_id TEXT NOT NULL,
    game_id TEXT NOT NULL,
    bet_type TEXT NOT NULL,
    bet_amount INTEGER NOT NULL,
    bet_line REAL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, game_id, bet_type)
);
```

## Performance Considerations

- **Binary Size**: Kotlin/Wasm produces larger binaries than JS
- **Cold Start**: Still faster than JVM containers
- **Memory Usage**: V8 isolates use less memory than JVM
- **Compute Limits**: 30-second execution limit per request

## Migration Strategy

### Phase 1: Core Commands (Current POC)
- Basic slash command handling
- Simple responses without database

### Phase 2: Database Integration  
- D1 setup and schema migration
- Core data operations (schedules, betting)

### Phase 3: Service Migration
- Weather service with Workers fetch API
- External API integrations (ESPN, etc.)

### Phase 4: Advanced Features
- Caching with Workers KV
- Image processing with Cloudflare Images
- Analytics with Workers Analytics Engine

## Challenges & Solutions

### Challenge: No Persistent WebSocket
**Solution**: Use Discord Interactions API webhooks instead of Gateway

### Challenge: Database Migration Complexity
**Solution**: Gradual migration with D1 and simplified schema

### Challenge: Binary Size
**Solution**: Code splitting and wasm-opt optimization

### Challenge: Spring Boot Dependencies
**Solution**: Replace with lightweight alternatives or manual implementation

## Next Steps

1. Test the POC with actual Discord webhook
2. Set up D1 database and migrate core tables
3. Adapt weather service for Workers environment
4. Performance testing and optimization
5. Production deployment strategy