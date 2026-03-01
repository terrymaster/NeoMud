---
name: worldmaker
description: Launch an AI world designer to test the NeoMud Maker editor through browser interaction
context: fork
agent: worldmaker
---

# Test NeoMud Maker Editor

Launch a WorldMaker session using the Playwright MCP browser tools. The worldmaker agent will open a real browser, navigate the Maker editor, take screenshots, and provide feedback from a world designer's perspective.

## Instructions

$ARGUMENTS

If arguments were provided above, focus your testing session on that specific area (e.g., "zone editor", "item CRUD", "NPC creation", "export flow"). Explore naturally but concentrate your effort and feedback on the requested system.

If no arguments were provided, run an exploratory session: navigate every section, try creating and editing entities, test the zone map editor, check visual polish — evaluate the editor as a new world designer would.

## Prerequisites

First, check if the Maker dev server is running:

```bash
curl -s http://localhost:5173 > /dev/null 2>&1 && echo "Maker is running" || echo "Maker is NOT running"
```

If the Maker is not running, inform the user that it needs to be started:

```
The Maker dev server is not running. Please start it first:
  cd maker && npm run dev
```

Do NOT proceed until the Maker is confirmed running.

## Session Flow

1. Navigate to `http://localhost:5173` using `browser_navigate`
2. Take an initial screenshot to see the landing page
3. Follow your methodology — look, understand, assess, decide, act, evaluate, report
4. File GitHub issues for every bug and UX problem you encounter
5. End with a structured WorldMaker report
