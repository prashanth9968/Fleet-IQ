@echo off
cd /d "C:\Users\naram\.gemini\antigravity\scratch"
git add */Dockerfile
git commit -m "fix: Use multi-stage Dockerfiles for Render deployment"
git push
