---
name: bugfixer
description: Work through the GitHub issue backlog fixing bugs automatically
context: fork
agent: bugfixer
---

# Fix Bugs from GitHub Backlog

Launch the BugFixer agent to work through open GitHub issues, fixing bugs and minor issues automatically. Architectural changes are flagged rather than attempted.

## Instructions

$ARGUMENTS

If arguments were provided above, focus on those specific issue numbers (e.g., "92 93 94" or "#92-#96"). Otherwise, work through all open bugs in priority order.

## Workflow

1. Fetch the open issue backlog with `gh issue list --state open --limit 30`
2. Skip issues labeled `enhancement` that require significant new features — note them in your report
3. For each bug: read the issue, investigate the code, fix it, write tests
4. If a fix requires architectural changes, comment on the issue explaining why and skip it
5. Run the full test suite after all fixes
6. Report what was fixed, skipped, and why — do NOT commit
