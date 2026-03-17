#!/bin/bash
set -e

# Deployment script for server
# This script runs on the deployment server

echo "Starting deployment..."

# Navigate to app directory
cd /opt/superdoc || cd ~/superdoc || { echo "App directory not found!"; exit 1; }

# Pull latest images
echo "Pulling latest Docker images..."
docker pull "$DOCKER_USERNAME/superdoc-backend:latest"
docker pull "$DOCKER_USERNAME/superdoc-frontend:latest"

# Stop and remove old containers
echo "Stopping old containers..."
docker-compose down || true

# Start new containers
echo "Starting new containers..."
docker-compose up -d

# Clean up old images
echo "Cleaning up old images..."
docker image prune -f

echo "Deployment complete!"
echo "Checking container status..."
docker-compose ps

