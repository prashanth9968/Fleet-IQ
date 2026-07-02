@echo off
cd /d "C:\Users\naram\.gemini\antigravity\scratch"

:: 1. Copy seed data from auth-service to vehicle-service as V007
copy "C:\Users\naram\.gemini\antigravity\scratch\fleetiq-auth-service\src\main\resources\db\migration\V003__seed_data.sql" "C:\Users\naram\.gemini\antigravity\scratch\fleetiq-vehicle-service\src\main\resources\db\migration\V007__seed_data.sql"

:: 2. Disable Flyway in auth-service
powershell -Command "$c = Get-Content 'fleetiq-auth-service\src\main\resources\application.yml' -Raw; $c = $c -replace 'flyway:\s*\n\s*enabled:\s*true', 'flyway:\n    enabled: false'; Set-Content 'fleetiq-auth-service\src\main\resources\application.yml' -Value $c"

:: 3. Disable Flyway in driver-service
powershell -Command "$c = Get-Content 'fleetiq-driver-service\src\main\resources\application.yml' -Raw; $c = $c -replace 'flyway:\s*\n\s*enabled:\s*true', 'flyway:\n    enabled: false'; Set-Content 'fleetiq-driver-service\src\main\resources\application.yml' -Value $c"

:: 4. Disable Flyway in auth-service application-prod.yml just in case
powershell -Command "$c = Get-Content 'fleetiq-auth-service\src\main\resources\application-prod.yml' -Raw; $c = $c -replace 'flyway:\s*\n\s*enabled:\s*true', 'flyway:\n    enabled: false'; Set-Content 'fleetiq-auth-service\src\main\resources\application-prod.yml' -Value $c"

:: 5. Disable Flyway in driver-service application-prod.yml just in case
powershell -Command "$c = Get-Content 'fleetiq-driver-service\src\main\resources\application-prod.yml' -Raw; $c = $c -replace 'flyway:\s*\n\s*enabled:\s*true', 'flyway:\n    enabled: false'; Set-Content 'fleetiq-driver-service\src\main\resources\application-prod.yml' -Value $c"

:: 6. Git commit and push
git add .
git commit -m "fix: Consolidate migrations into vehicle-service and disable Flyway in auth and driver to prevent DB conflicts"
git push
