#!/usr/bin/env bash
set -e

GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${CYAN}===========================================${NC}"
echo -e "${CYAN}   Starting Nextflow Tower (Single-User)   ${NC}"
echo -e "${CYAN}===========================================${NC}"

# Find suitable Java Home
if [ -d "$HOME/.jvms/jdk-11.0.22+7" ]; then
    export JAVA_HOME="$HOME/.jvms/jdk-11.0.22+7"
    export PATH="$JAVA_HOME/bin:$PATH"
fi

# Stop existing processes on 8000/8080
echo -e "${YELLOW}[1/4] Cleaning existing instances on ports 8000 and 8080...${NC}"
fuser -k 8000/tcp 2>/dev/null || pkill -9 -f "ng serve" 2>/dev/null || true
fuser -k 8080/tcp 2>/dev/null || pkill -9 -f "tower-backend" 2>/dev/null || true
sleep 1

# Start Backend
echo -e "${YELLOW}[2/4] Launching Nextflow Tower Backend (Port 8080)...${NC}"
./gradlew tower-backend:run > /dev/null 2>&1 &
BACKEND_PID=$!

# Wait for backend readiness
echo -n -e "${YELLOW}Waiting for backend readiness...${NC}"
for i in {1..30}; do
    if curl -s http://localhost:8080/service-info > /dev/null 2>&1; then
        echo -e " ${GREEN}Ready!${NC}"
        break
    fi
    echo -n "."
    sleep 1
done

# Start Frontend
echo -e "${YELLOW}[3/4] Launching Nextflow Tower Web UI (Port 8000)...${NC}"
cd tower-web
if command -v bun &> /dev/null; then
    NODE_OPTIONS=--openssl-legacy-provider bun run livedev > /dev/null 2>&1 &
else
    NODE_OPTIONS=--openssl-legacy-provider npm run livedev > /dev/null 2>&1 &
fi
cd ..

echo -e "${GREEN}===========================================${NC}"
echo -e "${GREEN}   Nextflow Tower is running!              ${NC}"
echo -e "${GREEN}===========================================${NC}"
echo -e "  Local UI:      ${CYAN}http://localhost:8000${NC}"
echo -e "  Network UI:    ${CYAN}http://192.168.86.54:8000${NC}"
echo -e "  API Endpoint:  ${CYAN}http://localhost:8080${NC}"
echo -e "==========================================="
echo -e "Nextflow Command Example:"
echo -e "  ${YELLOW}nextflow run hello -with-tower http://localhost:8080${NC}"
