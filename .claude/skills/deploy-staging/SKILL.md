---
name: deploy-staging
description: Deploy latest code to staging, wait for CI/CD pipelines, and verify health. Use after committing changes that need to go live on staging.
---

# Deploy to Staging

Check, trigger, and monitor staging deployments across both NeoMud repos. Verifies everything is healthy after deploy.

## Instructions

$ARGUMENTS

### Step 1: Check Pipeline Status

Check all active and recent deploy workflows across the NeoMud repo:

```bash
gh run list --limit 8 --json name,status,conclusion,headSha,createdAt \
  --jq '.[] | select(.name | test("Deploy|deploy")) | "\(.name) | \(.status) \(.conclusion) | \(.headSha[0:7]) | \(.createdAt[11:16])"'
```

And the NeoMud-Platform repo:
```bash
cd /c/Users/lbarnes/IdeaProjects/NeoMud-Platform && \
gh run list --limit 5 --json name,status,conclusion,headSha \
  --jq '.[] | select(.name | test("Deploy|deploy")) | "\(.name) | \(.status) \(.conclusion) | \(.headSha[0:7])"'
```

### Step 2: Wait for In-Progress Pipelines

If any deploy workflows are `in_progress`, poll until completion:

```bash
RUNID=<id> && while true; do
  STATUS=$(gh run view $RUNID --json status,conclusion --jq '.status + " " + .conclusion')
  echo "$(date +%H:%M:%S) $STATUS"
  if echo "$STATUS" | grep -q "completed"; then break; fi
  sleep 20
done
```

### Step 3: Handle Failures

If a deploy failed:
1. Check logs: `gh run view <id> --log-failed 2>&1 | tail -20`
2. Common issues:
   - **SSH auth**: VPS_SSH_KEY_STAGING secret may be wrong
   - **SCP permission**: Run `ssh root@159.203.127.47 'chown -R neomud:neomud /srv/neomud-web-stage/ /srv/neomud-assets/'`
   - **Flaky test**: Re-run with `gh run rerun <id> --failed`
   - **GHCR pull denied**: PAT on VPS needs `read:packages` + `repo` scopes
3. Re-run: `gh run rerun <id> --failed`

### Step 4: Pull New Images on VPS (if needed)

If the deploy workflow doesn't auto-recreate containers:

```bash
ssh root@159.203.127.47 'su - neomud -c "cd ~/neomud-staging && docker compose pull && docker compose up -d --force-recreate"'
```

### Step 5: Verify Staging Health

After deploy completes, verify all services:

```bash
# API health
curl -s https://stage-api.neomud.app/api/v1/health

# Web marketplace
curl -s -o /dev/null -w "web: %{http_code}\n" https://stage.neomud.app/

# Maker
curl -s -o /dev/null -w "maker: %{http_code}\n" https://stage-maker.neomud.app/

# Game server health
curl -s -o /dev/null -w "game: %{http_code}\n" https://stage.neomud.app/health

# Container status
ssh root@159.203.127.47 'docker ps --format "{{.Names}} {{.Status}}" | grep -E "stg|world"'
```

### Step 6: Verify WASM Client Content

Check the deployed WASM has the latest code:

```bash
ssh root@159.203.127.47 'ls -lt /srv/neomud-web-stage/client/neomud.js | head -1'
```

### Staging URLs
- **Marketplace**: https://stage.neomud.app/
- **API**: https://stage-api.neomud.app/api/v1/health
- **Maker**: https://stage-maker.neomud.app/
- **Game health**: https://stage.neomud.app/health

### VPS Access
- SSH: `ssh root@159.203.127.47`
- Deploy user: `neomud`
- Staging compose: `/home/neomud/neomud-staging/`
- Web files: `/srv/neomud-web-stage/`
- WASM client: `/srv/neomud-web-stage/client/`
- World bundles: `/srv/neomud-assets/`
