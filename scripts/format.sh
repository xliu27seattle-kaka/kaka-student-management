#!/usr/bin/env bash
# Formats the Java sources of both Maven modules with Spotless (google-java-format).
# Runs inside Docker; the formatted files are written back into each module's src folder.
#
# Usage: scripts/format.sh
set -euo pipefail
cd "$(dirname "$0")/.."

for module in backend notification-service; do
  echo "Formatting $module"
  docker build --quiet --target format-export --output "type=local,dest=$module/src" "$module" > /dev/null
done
echo "Done."
