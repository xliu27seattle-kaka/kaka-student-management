#!/usr/bin/env bash
# Runs the unit tests of one module inside Docker, so no local JDK or Node is needed.
# The sources are sent to Docker as a build context (no folder is mounted).
#
# Usage: scripts/test.sh <backend|notification-service|frontend> [extra maven args]
#   scripts/test.sh backend
#   scripts/test.sh backend -Dtest=AuthServiceImplTest
#   scripts/test.sh frontend
set -euo pipefail
cd "$(dirname "$0")/.."

module="${1:-}"
if [[ -z "$module" || ! -f "$module/Dockerfile" ]]; then
  echo "usage: scripts/test.sh <backend|notification-service|frontend> [extra maven args]" >&2
  exit 2
fi
shift

if [[ "$module" == "frontend" ]]; then
  docker build --target test --progress=plain "$module"
else
  docker build --target test --progress=plain --build-arg "MVN_ARGS=$*" "$module"
fi
