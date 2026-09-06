# ArenaStrike

Real-time multiplayer tactical 3D battleground web game.

## Project structure

- `backend/` - Spring Boot API, STOMP/WebSocket endpoint, and persistence layer.
- `frontend/` - HTML5/JavaScript client and Three.js rendering boundary.
- `assets/models/` - supplied GLB game models.

## Local setup

1. Create a MySQL database named `arenastrike`.
2. Set `DB_USERNAME` and `DB_PASSWORD` as environment variables when needed.
3. Start the backend from `backend/` with `gradle bootRun` (or `.\gradlew bootRun` when the Gradle wrapper is generated).
4. Serve `frontend/` through a local HTTP server; do not open the module directly from `file://`.
