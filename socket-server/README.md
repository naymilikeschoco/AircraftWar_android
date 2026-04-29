# Server Module

This module is a standalone Java server (No Activity) for experiment 6.

It provides:
- Socket relay server for online battle score/death sync
- HTTP auth/config server for register/login/cloud sync

## Run

From project root:

- `./gradlew :socket-server:run` (default: socket `8989`, auth `8080`)
- `./gradlew :socket-server:run --args="9000 8088"` (custom: socketPort authPort)

Socket server waits for 2 players, then relays each line from one player to the other.

HTTP endpoints:
- `POST /register` form: `username`, `password`
- `POST /login` form: `username`, `password`
- `GET /config?token=...`
- `POST /config` form: `token`, `unlockedDifficulty`, `coins`, `audioEnabled`
