#!/bin/bash
set -euo pipefail

COMPOSE="docker compose"
APP_CONTAINER="eclipse-ocx-2026-app-1"
OLLAMA_CONTAINER="eclipse-ocx-2026-ollama-1"

pull_models() {
    echo "  Pulling AI models (first run only)..."
    $COMPOSE up -d ollama
    sleep 3
    docker exec $OLLAMA_CONTAINER ollama pull gemma3:4b 2>/dev/null || true
    docker exec $OLLAMA_CONTAINER ollama pull mistral 2>/dev/null || true
    docker exec $OLLAMA_CONTAINER ollama pull nomic-embed-text 2>/dev/null || true
}

deploy() {
    echo ""
    echo "  Starting deployment..."
    echo ""

    pull_models

    echo "  Starting PostgreSQL..."
    $COMPOSE up -d postgres

    echo "  Waiting for PostgreSQL..."
    until docker exec eclipse-ocx-2026-postgres-1 pg_isready -U ocx > /dev/null 2>&1; do
        sleep 1
    done
    echo "  PostgreSQL is ready"

    echo "  Building and deploying application..."
    $COMPOSE up -d --build app

    echo ""
    echo "  Application is ready"
    echo ""
    echo "  http://localhost:8080"
    echo ""
}

start() {
    $COMPOSE up -d
    echo "Services started. http://localhost:8080"
}

stop() {
    $COMPOSE down
    echo "Services stopped."
}

logs() {
    $COMPOSE logs -f app
}

status() {
    $COMPOSE ps
}

clean() {
    $COMPOSE down -v --rmi local
    echo "Everything removed including data and images."
}

truncate() {
    echo "  Truncating embeddings and documents..."
    docker exec eclipse-ocx-2026-postgres-1 psql -U ocx -c "TRUNCATE document_chunks, conference_talks RESTART IDENTITY CASCADE;" > /dev/null 2>&1
    echo "  Done. Restart the application to re-seed: ./run.sh restart"
}

restart() {
    stop
    sleep 2
    deploy
}

case "${1:-}" in
    deploy)   deploy ;;
    start)    start ;;
    stop)     stop ;;
    restart)  restart ;;
    logs)     logs ;;
    status)   status ;;
    clean)    clean ;;
    truncate) truncate ;;
    *)        echo "Usage: $0 {deploy|start|stop|restart|logs|status|clean|truncate}" ;;
esac
