<div align="center">
  <h1>🏆 Dynamic Leaderboard API</h1>
  <p>A flexible, real-time leaderboard REST API for game servers and applications.</p>
</div>

<br/>

## 📖 Overview

I built this backend service to dive deeper into Spring Boot and learn how to handle real-time scoring data efficiently. Instead of creating a hardcoded scoreboard for just one game, I wanted to build a flexible engine where any game client or application can register a new leaderboard, define its own scoring math, and submit player scores seamlessly.

## ✨ Key Features

- **Multi-Game Support**: You can create completely separate leaderboards dynamically for different games or categories (e.g., `pixel-racer`, `idle-clicker`) all hitting the same API.
- **Dynamic Scoring Formulas (SpEL)**: Instead of writing new Java code every time a game needs a new scoreboard, you can pass a math formula as a string when creating the leaderboard (e.g., `#kills * 2 + #assists - #deaths`). 
  - *How it works*: When a player submits their stats as a JSON payload, the application uses **Spring Expression Language (SpEL)** to dynamically plug those JSON values into the formula and calculate the final score on the fly.
- **Pluggable Ranking Strategies**: Different games determine the "winner" differently. When creating a leaderboard, you attach a Strategy to it. Under the hood, this uses the Strategy Design Pattern to sort the Redis sets correctly:
    - **Highest Score Wins (`DESCENDING`)**: Standard points-based games.
    - **Lowest Score Wins (`ASCENDING`)**: Time trials or speedruns.
    - **Closest to Target (`CLOSE_TO_X`)**: Guessing games where you want to be closest to a specific number. The API calculates the absolute distance between your score and the target.
- **Time-Based Rankings (Snapshots)**: Whenever a score is submitted, it automatically updating the **Global** leaderboard, as well as the **Monthly** and **Weekly** snapshots so players can see their recent performance.
- **Top Players & Specific Ranks**: Quickly fetch the top `N` players, or request a specific player's current rank and metadata.
- **Secured Endpoints**: Uses API keys to protect sensitive administrative endpoints while leaving data retrieval (querying the leaderboards) open to the public.

## 🛠️ Technology Stack

- **Java 21**: Leveraging the latest language features and records.
- **Spring Boot 4.0.2**: The core framework for the REST application, including Web, Data Redis, Validation, and Security modules.
- **Redis**: Chosen for real-time ranking using Sorted Sets (`ZSET`).
- **Spring Expression Language (SpEL)**: Used to parse and evaluate the dynamic scoring formulas inside the Java application.
- **Docker & Docker Compose**: Used to easily spin up the Redis dependencies and containerize the environment.
- **Testing**: Thoroughly tested using **JUnit 5** and **Mockito** to ensure the formula evaluator and scoring logic handle edge cases correctly.

<br/>

## 🌐 API Overview

### Admin Endpoints (Requires API Key)
- `POST /api/v1/leaderboards`
  - Goal: Registers a new leaderboard with specific logic, ranking strategies, and custom formulas.
- `DELETE /api/v1/leaderboards/{gameId}`
  - Path Variables: `gameId`
  - Goal: Completely wipes a leaderboard and its configuration.
- `POST /api/v1/leaderboards/{gameId}/scores`
  - Path Variables: `gameId`
  - Goal: Submits a player's raw metrics to be evaluated and inserted into the specified leaderboard.

### Public Endpoints (Read-Only)
- `GET /api/v1/leaderboards/{gameId}?type={GLOBAL|MONTHLY|WEEKLY}&offset={int}&limit={int}`
  - Path Variables: `gameId`
  - Query Parameters: `type` (default: `GLOBAL`), `offset` (default: `0`), `limit` (default: `10`)
  - Goal: Fetches a slice of the leaderboard (Top N players).
- `GET /api/v1/leaderboards/{gameId}/entries/{playerId}?type={GLOBAL|MONTHLY|WEEKLY}`
  - Path Variables: `gameId`, `playerId`
  - Query Parameters: `type` (default: `GLOBAL`)
  - Goal: Fetches the exact rank, evaluated score, and metadata for a specific player in a given game.
- `GET /api/v1/leaderboards/player/{playerId}`
  - Path Variables: `playerId`
  - Goal: Fetches all ranking entries across **all** games for a specific player ID.

<br/>

## 🚀 API Usage Examples

Here is a quick look at how to interact with the API, using the `guess-the-number` game as an example.

### 1. Create a Leaderboard
This creates a leaderboard where players try to guess a number closest to `500.0`.

**Request:** `POST /api/v1/leaderboards`  
**Headers:** `Authorization: ApiKey <Your-API-Key>`
```json
{
  "gameId": "guess-the-number",
  "rankingStrategy": {
    "name": "CLOSE_TO_X",
    "target": 500.0
  },
  "target": 500.0,
  "formula": {
    "expression": "#guess",
    "variables": ["guess"]
  },
  "metadata": {
    "event_name": "Weekend Riddle",
    "difficulty": "Hard",
    "prize_pool": "1000 Coins"
  }
}
```

### 2. Submit a Score (Guess)
A player submits their guess for the game.

**Request:** `POST /api/v1/leaderboards/guess-the-number/scores`  
**Headers:** `Authorization: ApiKey <Your-API-Key>`
```json
{
  "playerId": "bob",
  "values": {
    "guess": 510.0
  },
  "metadata": {
    "attempts": "2"
  }
}
```

### 3. Fetch Top Players (Global)
Retrieve the current standings for the game. Notice the API automatically ranks Bob since his guess was `510` (only 10 away from the `500` target).

**Request:** `GET /api/v1/leaderboards/guess-the-number?type=GLOBAL&offset=0&limit=10`
```json
{
    "gameId": "guess-the-number",
    "type": "GLOBAL",
    "metadata": {
        "prize_pool": "1000 Coins",
        "difficulty": "Hard",
        "event_name": "Weekend Riddle"
    },
    "pagination": {
        "offset": 0,
        "limit": 10,
        "count": 3,
        "total": 3
    },
    "entries": [
        {
            "playerId": "bob",
            "score": 510.0,
            "rank": 1,
            "metadata": {
                "attempts": "2"
            }
        },
        {
            "playerId": "alice",
            "score": 450.0,
            "rank": 2,
            "metadata": {
                "attempts": "1"
            }
        },
        {
            "playerId": "charlie",
            "score": 600.0,
            "rank": 3,
            "metadata": {
                "attempts": "1"
            }
        }
    ]
}
```
