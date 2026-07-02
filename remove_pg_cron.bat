@echo off
cd /d "C:\Users\naram\.gemini\antigravity\scratch"
powershell -Command "$files = @('C:\Users\naram\.gemini\antigravity\scratch\fleetiq-auth-service\src\main\resources\db\migration\V001__extensions_and_schemas.sql','C:\Users\naram\.gemini\antigravity\scratch\fleetiq-database\migrations\V001__extensions_and_schemas.sql','C:\Users\naram\.gemini\antigravity\scratch\fleetiq-tracking-service\src\main\resources\db\migration\V001__extensions_and_schemas.sql','C:\Users\naram\.gemini\antigravity\scratch\fleetiq-vehicle-service\src\main\resources\db\migration\V001__extensions_and_schemas.sql'); foreach ($file in $files) { (Get-Content $file) -replace 'CREATE EXTENSION IF NOT EXISTS pg_cron;', '-- CREATE EXTENSION IF NOT EXISTS pg_cron;' | Set-Content $file }"
git add .
git commit -m "fix: Disable pg_cron extension creation for Neon compatibility"
git push
