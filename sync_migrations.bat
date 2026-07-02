@echo off
cd /d "C:\Users\naram\.gemini\antigravity\scratch"
powershell -Command "$source = 'C:\Users\naram\.gemini\antigravity\scratch\fleetiq-vehicle-service\src\main\resources\db\migration\V001__extensions_and_schemas.sql'; Get-ChildItem -Path . -Filter 'V001__extensions_and_schemas.sql' -Recurse | Where-Object { $_.FullName -ne $source } | ForEach-Object { Copy-Item -Path $source -Destination $_.FullName -Force }"
git add .
git commit -m "fix: Synchronize V001 migrations across all services to match DB checksum"
git push
