---
name: web-uat-test
description: Launch a browser-based UAT tester for the NeoMud web marketplace and WASM game client via Playwright
context: fork
agent: web-uat-tester
---

# Web UAT Test — NeoMud Marketplace & WASM Client

Launch a BrowserKnight session using the Playwright MCP browser tools. The agent will open a real browser, navigate the marketplace and WASM game client, take screenshots, play the game, and file bugs from a player's perspective.

## Instructions

$ARGUMENTS

If arguments were provided above, focus your testing session on that specific area. Examples:
- `marketplace` — test only the React marketplace (landing, search, detail, responsive)
- `play transition` — test the flow from marketplace → WASM game loading → game
- `game combat` — get into the game and test combat flow
- `full journey` — complete end-to-end: browse → select world → play → fight → rate

If no arguments were provided, run a **full journey** exploratory session.

## Prerequisites

### For Marketplace Testing

Check if the marketplace dev server is running:

```bash
curl -s http://localhost:3000 > /dev/null 2>&1 && echo "Marketplace is running on :3000" || echo "Marketplace is NOT running"
```

If not running, inform the user:
```
The marketplace dev server is not running. Please start it:
  cd NeoMud-Platform/web && npm run dev
```

### For Game Testing (Play transition + WASM)

Check if the game server is running:

```bash
curl -s http://localhost:8080/health 2>&1
```

If the game server is not running and the user wants to test the play flow, inform them:
```
The game server is not running. Please start it:
  ./gradlew packageWorld --rerun-tasks
  ./gradlew :server:run
```

Check if the Platform API is running (needed for marketplace → play flow):
```bash
curl -s http://localhost:3002/api/v1/health 2>&1
```

If the Platform API is not running, the marketplace will show errors loading worlds. For marketplace-only visual testing this is acceptable (test error states). For play-through testing, inform the user:
```
The Platform API is not running. Please start it:
  cd NeoMud-Platform && npm run dev
```

### GitHub Label Setup

Ensure the `web-uat` label exists:
```bash
gh label create web-uat --color 1D76DB --description "Web UAT testing findings" 2>/dev/null || true
```

## Session Flow

1. Run prerequisite checks above
2. Navigate to `http://localhost:3000` using `browser_navigate`
3. Take an initial screenshot
4. Follow your methodology — look, understand, assess, decide, act, wait, evaluate, report
5. File GitHub issues with screenshots for every bug and UX problem
6. End with a structured Web UAT report

## Cleanup (MANDATORY)

After the session is complete (after producing the report), you MUST run these cleanup commands:

```bash
# Delete all screenshots cached during the session
rm -rf .playwright-mcp/*
rm -f web-uat-*.png
```
