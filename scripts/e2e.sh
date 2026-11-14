#!/usr/bin/env bash
# Runs the Playwright end-to-end tests against the running stack (docker compose up).
# Needs Node.js and Google Chrome on this machine.
#
# Usage: scripts/e2e.sh [playwright args]
#   scripts/e2e.sh
#   scripts/e2e.sh tests/register.spec.ts
set -euo pipefail
cd "$(dirname "$0")/../e2e"

if [[ ! -d node_modules ]]; then
  npm ci
fi
npx playwright test "$@"
