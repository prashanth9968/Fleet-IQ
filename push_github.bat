@echo off
cd /d "C:\Users\naram\.gemini\antigravity\scratch"

git add README.md
git commit -m "docs: Add comprehensive README.md"

git remote add origin https://github.com/prashanth9968/Fleet-IQ

echo ========================================================
echo Pushing repository to GitHub...
echo If prompted, please authenticate with your GitHub account.
echo ========================================================

git push -u origin master
