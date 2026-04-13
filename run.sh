#!/bin/bash
set -e

echo "=== Eclipse OCX 2026 - Intelligent Monolith Setup ==="

# 1. Start Ollama and PostgreSQL
echo "Starting Ollama and PostgreSQL..."
docker compose up -d ollama postgres

# 2. Wait for PostgreSQL
echo "Waiting for PostgreSQL..."
until docker compose exec postgres pg_isready -U ocx -d ocx > /dev/null 2>&1; do
    sleep 1
done
echo "PostgreSQL is ready."

# 3. Enable pgvector extension
echo "Enabling pgvector extension..."
docker compose exec postgres psql -U ocx -d ocx -c "CREATE EXTENSION IF NOT EXISTS vector;"

# 4. Pull required models
echo "Pulling gemma4:e2b..."
docker compose exec ollama ollama pull gemma4:e2b

echo "Pulling nomic-embed-text..."
docker compose exec ollama ollama pull nomic-embed-text

# 5. Build WAR
echo "Building WAR..."
./mvnw clean package -DskipTests

# 6. Build and start app
echo "Building Docker image and starting Payara..."
docker compose up -d app --build

echo ""
echo "=== Ready! Open http://localhost:8080 ==="
