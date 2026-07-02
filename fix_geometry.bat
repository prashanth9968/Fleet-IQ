@echo off
cd /d "C:\Users\naram\.gemini\antigravity\scratch"
powershell -Command "Get-ChildItem -Path . -Filter *.java -Recurse | ForEach-Object { $c = Get-Content $_.FullName -Raw; if ($c -match '\"geometry\(Point, 4326\)\"') { $c = $c -replace '\"geometry\(Point, 4326\)\"', '\"geography(Point, 4326)\"'; Set-Content $_.FullName -Value $c } }"
git add .
git commit -m "fix: Align JPA location column definitions with geography DB schema"
git push
