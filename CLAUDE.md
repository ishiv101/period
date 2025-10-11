# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**LunaCycle** is a period tracking web application built with Java backend and vanilla JavaScript frontend. The app provides cycle tracking, AI-powered health advice via Google Gemini API, and a community forum.

## Architecture

### Two Server Implementations

The codebase has two distinct server entry points:

1. **MainServer.java** (period_site/MainServer.java:10) - Web server with static file serving, chatbot, and forum endpoints. Runs on port 8080.

2. **Calendar/Main.java** (period_site/Calendar/Main.java:13) - Standalone cycle tracker API with port fallback logic. Attempts to bind to port 8000 (or PORT env var), with automatic fallback to next available port if busy.

### Core Components

**Calendar Logic** (period_site/Calendar/CalendarLogic.java:4)
- Handles cycle date calculations using a simple 28-day cycle model
- Methods: `dayOfCycle()`, `nextPeriodDate()`, `cyclePhase()`
- Phase labels: Menstrual (days 1-5), Follicular (6-14), Ovulation (15-17), Luteal (18-28)

**Gemini API Integration**
- Main.java:44 contains `getGeminiResponse()` for AI cycle advice
- API key currently hardcoded in Main.java:11 (should migrate to .env via EnvLoader)
- ChatBot.java and utils/EnvLoader.java are placeholder files (not implemented)

**Frontend**
- Static files in `public/` directory
- calendar.js demonstrates fetch to `http://localhost:8000/cycle` endpoint
- Separate HTML pages: index.html, chat.html, forum.html, calendar.html

## Common Commands

### Running the Servers

**Primary web server (port 8080):**
```bash
cd period_site
javac MainServer.java Forum.java ChatBot.java
java MainServer
```

**Cycle tracker API (port 8000 with fallback):**
```bash
cd period_site/Calendar
javac CalendarLogic.java Main.java
java Main
# Or with custom port: java Main 3000
# Or via env: PORT=3000 java Main
```

**CLI version (deprecated, has syntax errors):**
```bash
cd period_site
javac Main.java
java Main
# Note: This file has compilation errors and appears to be superseded by Calendar/Main.java
```

### Development Workflow

**Compile all Java files:**
```bash
cd period_site
javac *.java Calendar/*.java utils/*.java
```

**Run from project root:**
```bash
cd period_site && java MainServer
```

## Key Development Notes

### API Endpoints

- `GET /` - Serves static files from `public/` directory (MainServer) or embedded HTML demo (Calendar/Main)
- `POST /api/chat` - Chatbot endpoint (calls ChatBot.askGemini)
- `GET|POST /api/forum` - Forum endpoint (handled by Forum.handleForum)
- `GET /cycle?last=YYYY-MM-DD` - Cycle info JSON API (Calendar/Main)
- `GET /health` - Health check endpoint (Calendar/Main)

### Environment Configuration

- `.env` file exists in period_site/ but EnvLoader.java is not implemented
- API key is currently hardcoded in Main.java:11
- Port configuration via PORT environment variable or CLI argument (Calendar/Main only)

### Code Organization Issues

- Multiple Main.java files (period_site/Main.java and period_site/Calendar/Main.java) serve different purposes
- CalendarLogic.java exists in both deleted state and active Calendar/ directory
- Some files (ChatBot.java, Forum.java, chat.js, forum.js) are stubs with only comments
- Git status shows deleted files that may indicate incomplete refactoring

### CORS Configuration

Calendar/Main.java includes CORS headers (`Access-Control-Allow-Origin: *`) for cross-origin requests, useful for frontend development on different ports.
