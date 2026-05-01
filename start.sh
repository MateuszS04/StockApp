#!/bin/sh

set -e
PORT="${1:-8080}"

exec java -jar /app/app.jar --server.port="$PORT"