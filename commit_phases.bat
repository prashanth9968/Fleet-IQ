@echo off
cd /d "C:\Users\naram\.gemini\antigravity\scratch"

git init
git config user.email "bot@fleetiq.local"
git config user.name "FleetIQ Agent"

git add .gitignore fleetiq-engineering-standards.md
git commit -m "chore: Initialize project and standards"

git add fleetiq-auth-service
git commit -m "feat(phase-1): Add Auth Service"

if exist fleetiq-database (
    git add fleetiq-database
    git commit -m "feat(phase-2): Add Database configurations"
)

git add fleetiq-vehicle-service
git commit -m "feat(phase-3): Add Vehicle Service"

git add fleetiq-device-gateway-service
git commit -m "feat(phase-4): Add Device Gateway Service"

git add fleetiq-tracking-service
git commit -m "feat(phase-5): Add Tracking Service"

git add fleetiq-fuel-service
git commit -m "feat(phase-6): Add Fuel Service"

git add fleetiq-alerts-service
git commit -m "feat(phase-7): Add Alerts and Geofencing Service"

git add fleetiq-driver-service fleetiq-admin-dashboard
git commit -m "feat(phase-8): Add Driver Service and React Dashboard"

git add fleetiq-vehicle-health-service
git commit -m "feat(phase-9): Add Vehicle Health Service"

git add fleetiq-analytics-service
git commit -m "feat(phase-10): Add Analytics Service"

git add fleetiq_mobile
git commit -m "feat(phase-11): Add Flutter Mobile Apps Monorepo"

git add fleetiq-api-gateway docker-compose.yml k8s .github prometheus.yml mosquitto.conf DEPLOYMENT.md
git commit -m "feat(phase-12): Add API Gateway, Docker, K8s, CI/CD pipelines"

git log --oneline
