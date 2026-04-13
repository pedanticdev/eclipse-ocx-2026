#!/bin/bash
set -e

# Eclipse OCX 2026 - Jakarta EE AI Assistant

GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

ok()   { echo -e "${GREEN}✓${NC} $1"; }
err()  { echo -e "${RED}✗${NC} $1"; }
info() { echo -e "${BLUE}ℹ${NC} $1"; }
warn() { echo -e "${YELLOW}⚠${NC} $1"; }

dc() { docker compose "$@"; }

check_docker() {
    if ! docker info &> /dev/null; then
        err "Docker is not running. Start Docker and try again."
        exit 1
    fi
}

wait_for_postgres() {
    info "Waiting for PostgreSQL..."
    local attempts=0
    while ! dc exec -T postgres pg_isready -U ocx -d ocx &> /dev/null; do
        sleep 1
        attempts=$((attempts + 1))
        if [ $attempts -gt 30 ]; then
            err "PostgreSQL did not become ready in time."
            exit 1
        fi
    done
    ok "PostgreSQL is ready"
}

wait_for_app() {
    info "Waiting for Payara Micro..."
    local attempts=0
    while ! curl -sf http://localhost:8080/ > /dev/null 2>&1; do
        sleep 2
        attempts=$((attempts + 1))
        if [ $attempts -gt 60 ]; then
            err "Application did not start in time. Check logs: ./run.sh logs"
            exit 1
        fi
    done
    ok "Application is ready"
}

setup_infra() {
    info "Starting PostgreSQL and Ollama..."
    dc up -d postgres ollama
    wait_for_postgres

    info "Enabling pgvector extension..."
    dc exec -T postgres psql -U ocx -d ocx -c "CREATE EXTENSION IF NOT EXISTS vector;"

    info "Pulling AI models (first run only)..."
    dc exec -T ollama ollama pull gemma4:e2b
    dc exec -T ollama ollama pull nomic-embed-text
    ok "Infrastructure ready"
}

cmd_deploy() {
    check_docker
    setup_infra

    info "Building and deploying application..."
    dc up -d app --build

    wait_for_app
    echo ""
    echo "  http://localhost:8080"
}

cmd_start() {
    check_docker
    info "Starting services..."
    dc up -d
    wait_for_app
    echo ""
    ok "http://localhost:8080"
}

cmd_stop() {
    check_docker
    info "Stopping services..."
    dc down
    ok "Stopped"
}

cmd_restart() {
    cmd_stop
    cmd_deploy
}

cmd_truncate() {
    check_docker
    if ! dc ps --status running postgres | grep -q postgres; then
        err "PostgreSQL is not running. Start with: ./run.sh start"
        exit 1
    fi
    info "Truncating embeddings table..."
    dc exec -T postgres psql -U ocx -d ocx -c "TRUNCATE embeddings;"
    ok "Embeddings cleared"
    info "Restarting application to re-ingest..."
    dc restart app
    wait_for_app
    echo ""
    ok "Re-ingestion complete. http://localhost:8080"
}

cmd_logs() {
    check_docker
    dc logs -f app
}

cmd_status() {
    check_docker
    echo ""
    if curl -sf http://localhost:8080/ > /dev/null 2>&1; then
        ok "Application is running at http://localhost:8080"
    else
        err "Application is not responding"
    fi
    echo ""
    dc ps
}

cmd_clean() {
    check_docker
    warn "Removing all containers, volumes, and data..."
    dc down -v
    docker system prune -f
    ok "Cleaned"
}

cmd_help() {
    echo ""
    echo "  Eclipse OCX 2026 - Jakarta EE AI Assistant"
    echo ""
    echo "  Usage: ./run.sh <command>"
    echo ""
    echo "  Commands:"
    echo "    deploy    Full setup: infra + build + deploy (first run)"
    echo "    start     Start existing services"
    echo "    stop      Stop all services"
    echo "    restart   Stop and redeploy"
    echo "    truncate  Clear embeddings and re-ingest"
    echo "    logs      Tail application logs"
    echo "    status    Show service status"
    echo "    clean     Remove everything including data"
    echo ""
}

case "${1:-help}" in
    deploy)   cmd_deploy   ;;
    start)    cmd_start    ;;
    stop)     cmd_stop     ;;
    restart)  cmd_restart  ;;
    truncate) cmd_truncate ;;
    logs)     cmd_logs     ;;
    status)   cmd_status   ;;
    clean)    cmd_clean    ;;
    help|--help|-h) cmd_help ;;
    *)
        echo "Unknown command: $1"
        cmd_help
        exit 1
        ;;
esac
