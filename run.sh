#!/usr/bin/env bash
# Nextflow Tower Native & Podman Launcher Script

set -e

GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

echo -e "${CYAN}===================================================${NC}"
echo -e "${CYAN}   Nextflow Tower Launcher (Native / Podman)     ${NC}"
echo -e "${CYAN}===================================================${NC}"

# Configure Java Environment
if [ -d "$HOME/.jvms/jdk-11.0.22+7" ]; then
    export JAVA_HOME="$HOME/.jvms/jdk-11.0.22+7"
    export PATH="$JAVA_HOME/bin:$PATH"
elif [ -d "$HOME/.jvms/jdk-17.0.10+7" ]; then
    export JAVA_HOME="$HOME/.jvms/jdk-17.0.10+7"
    export PATH="$JAVA_HOME/bin:$PATH"
fi

# Function to run native
run_native() {
    echo -e "${YELLOW}[1/4] Stopping any existing instances on ports 8000 and 8080...${NC}"
    fuser -k 8000/tcp 2>/dev/null || pkill -9 -f "ng serve" 2>/dev/null || true
    fuser -k 8080/tcp 2>/dev/null || pkill -9 -f "tower-backend" 2>/dev/null || true
    pkill -9 -f "java.*tower-backend" 2>/dev/null || true
    sleep 1

    echo -e "${YELLOW}[2/4] Starting Nextflow Tower Backend (Port 8080)...${NC}"
    ./gradlew tower-backend:run > "$ROOT_DIR/backend.log" 2>&1 &
    BACKEND_PID=$!
    echo "Backend PID: $BACKEND_PID"

    echo -n -e "${YELLOW}Waiting for backend readiness...${NC}"
    for i in {1..30}; do
        if curl -s http://localhost:8080/service-info > /dev/null 2>&1; then
            echo -e " ${GREEN}Ready!${NC}"
            break
        fi
        echo -n "."
        sleep 1
    done

    echo -e "${YELLOW}[3/4] Starting Nextflow Tower Web UI (Port 8000)...${NC}"
    cd tower-web
    if command -v bun &> /dev/null; then
        NODE_OPTIONS=--openssl-legacy-provider bun run livedev > "$ROOT_DIR/frontend.log" 2>&1 &
    else
        NODE_OPTIONS=--openssl-legacy-provider npm run livedev > "$ROOT_DIR/frontend.log" 2>&1 &
    fi
    FRONTEND_PID=$!
    cd "$ROOT_DIR"

    echo -n -e "${YELLOW}Waiting for web UI readiness...${NC}"
    for i in {1..35}; do
        if curl -s http://localhost:8000 > /dev/null 2>&1; then
            echo -e " ${GREEN}Ready!${NC}"
            break
        fi
        echo -n "."
        sleep 1
    done
}

# Function to run containerized via Podman / Docker
run_podman() {
    echo -e "${YELLOW}Launching via Podman/Docker compose...${NC}"
    if command -v podman-compose &> /dev/null; then
        podman-compose up -d
    elif command -v docker-compose &> /dev/null; then
        docker-compose up -d
    elif command -v docker &> /dev/null; then
        docker compose up -d
    else
        echo -e "${RED}Error: Neither podman-compose nor docker-compose found!${NC}"
        exit 1
    fi
}

# Select Mode
MODE="${1:-native}"

if [ "$MODE" == "podman" ] || [ "$MODE" == "docker" ] || [ "$MODE" == "container" ]; then
    run_podman
else
    run_native
fi

# Detect Local & Network IPs
LOCAL_IP=$(hostname -I 2>/dev/null | awk '{print $1}' || echo "127.0.0.1")
WIN_IP=$(/mnt/c/Windows/System32/ipconfig.exe 2>/dev/null | grep -E -i 'IPv4|192\.168' | grep -v '172.' | head -n1 | awk -F': ' '{print $2}' | tr -d '\r' || echo "")

echo -e "\n${GREEN}===================================================${NC}"
echo -e "${GREEN}   Nextflow Tower is UP and RUNNING!              ${NC}"
echo -e "${GREEN}===================================================${NC}"
echo -e "  Local UI:       ${CYAN}http://localhost:8000${NC}"
[ -n "$LOCAL_IP" ] && echo -e "  WSL / Linux IP:  ${CYAN}http://${LOCAL_IP}:8000${NC}"
[ -n "$WIN_IP" ]   && echo -e "  Windows LAN IP:  ${CYAN}http://${WIN_IP}:8000${NC}"
echo -e "  API Endpoint:   ${CYAN}http://localhost:8080${NC}"
echo -e "==================================================="
echo -e "Nextflow Execution Command:"
echo -e "  ${YELLOW}nextflow run hello -with-tower http://localhost:8080${NC}"
echo -e "===================================================\n"
