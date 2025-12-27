# Tombalator

A self-hosted Tombala game application. Tombala is the Turkish version of Bingo, played with numbers 1-90. This application allows you to host and play Tombala games with friends.

## Tech Stack

### Backend
- **Kotlin**: 2.2.21
- **Ktor**: 3.3.2 (Web framework)
- **Kotlinx Serialization**: 1.7.3
- **Logback**: 1.4.14
- **JVM Toolchain**: 21

### Frontend
- **React**: 19.2.0
- **TypeScript**: 5.9.3
- **Vite**: 7.2.4
- **Vite React Plugin (SWC)**: 4.2.2

## Features

- Generate random Tombala cards with proper number distribution
- Draw numbers from 1-90 pool
- Mark numbers on cards
- Win detection (First Row, Two Rows, Full House)
- Real-time game updates via WebSocket
- RESTful API for game management

## Project Structure

```
Tombalator/
├── server/              # Kotlin/Ktor backend
│   ├── src/main/kotlin/
│   │   ├── Application.kt
│   │   ├── Routing.kt
│   │   ├── models/      # Data models
│   │   └── game/        # Game logic
│   └── build.gradle.kts
└── tombalator-ui/       # React/TypeScript frontend
    ├── src/
    │   ├── App.tsx
    │   └── ...
    └── package.json
```

## Getting Started

### Prerequisites
- Java 21 or higher
- Node.js (for frontend)
- Gradle (or use the included Gradle wrapper)

### Running the Backend

```bash
cd server
./gradlew run
```

The server will start on `http://localhost:8080`

### Running the Frontend

```bash
cd tombalator-ui
npm install
npm run dev
```

The frontend will start on `http://localhost:5173` (default Vite port)

## API Endpoints

- `POST /api/game/create` - Create a new game
- `POST /api/game/join` - Join an existing game
- `POST /api/game/{gameId}/draw` - Draw the next number
- `POST /api/game/{gameId}/mark` - Mark a number on player's card
- `GET /api/game/{gameId}` - Get current game state
- `WS /ws/game/{gameId}` - WebSocket connection for real-time updates

## License

This project is open source and available for self-hosting.
