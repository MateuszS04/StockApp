@echo off
setlocal

set "PORT=%~1"
if "%PORT%"=="" set "PORT=8080"

echo Starting stockapp stack on http://localhost:%PORT%
docker compose up --build
endlocal
