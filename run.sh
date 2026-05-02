#!/bin/sh
set -e

PORT="${1:-8080}"

export PORT

echo "Starting stockapp stack on http://localhost:${PORT}"
exec docker compose up --build