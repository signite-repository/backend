#!/bin/bash

# 🚀 Signite Project Development Manager
# 전체 프로젝트 개발 워크플로우 관리 스크립트

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m'

echo -e "${PURPLE}🚀 Signite Project Development Manager${NC}"
echo

function show_help() {
    echo -e "${BLUE}Usage: ./dev.sh [COMMAND]${NC}"
    echo
    echo -e "${YELLOW}Backend Commands:${NC}"
    echo "  build       - Build backend application"
    echo "  test        - Run backend tests"
    echo "  docker      - Build Docker image with auto-versioning"
    echo "  deploy      - Deploy backend to K8s"
    echo "  dev         - Start development mode (logs + port-forward)"
    echo
    echo -e "${YELLOW}WebSocket Commands:${NC}"
    echo "  ws-build    - Build WebSocket server"
    echo "  ws-docker   - Build WebSocket Docker image"
    echo "  ws-deploy   - Deploy WebSocket server to K8s"
    echo "  ws-dev      - Start WebSocket development mode"
    echo "  ws-logs     - View WebSocket server logs"
    echo
    echo -e "${YELLOW}Full Stack Commands:${NC}"
    echo "  all-build   - Build both backend and WebSocket"
    echo "  all-docker  - Build both Docker images"
    echo "  all-deploy  - Deploy complete stack to K8s"
    echo "  all-dev     - Start full development mode"
    echo
    echo -e "${YELLOW}Infrastructure Commands:${NC}"
    echo "  setup       - Setup complete K8s environment"
    echo "  teardown    - Teardown K8s environment"
    echo "  status      - Check K8s deployment status"
    echo "  logs        - View application logs"
    echo
    echo -e "${YELLOW}Utility Commands:${NC}"
    echo "  secrets     - Generate secure K8s secrets"
    echo "  clean       - Clean all build artifacts"
    echo "  version     - Show version information"
    echo "  help        - Show this help"
    echo
}

function backend_build() {
    echo -e "${YELLOW}🔨 Building backend...${NC}"
    cd backend
    ./start.sh build
    cd ..
}

function backend_test() {
    echo -e "${YELLOW}🧪 Running backend tests...${NC}"
    cd backend
    ./start.sh test
    cd ..
}

function backend_docker() {
    echo -e "${YELLOW}🐳 Building backend Docker image...${NC}"
    cd backend
    ./start.sh docker
    cd ..
}

function backend_deploy() {
    echo -e "${YELLOW}🚢 Deploying backend...${NC}"
    cd backend
    ./start.sh deploy
    cd ..
}

function backend_dev() {
    echo -e "${YELLOW}💻 Starting backend development mode...${NC}"
    cd backend
    ./start.sh dev
    cd ..
}

function setup_k8s() {
    echo -e "${YELLOW}🏗️ Setting up complete K8s environment...${NC}"
    
    # 시크릿 생성
    echo -e "${BLUE}🔒 Generating secure secrets...${NC}"
    ./scripts/create-secrets.sh
    
    # 시크릿 배포
    echo -e "${BLUE}🔑 Deploying secrets...${NC}"
    kubectl apply -f k8s/mariadb/secret.yaml
    kubectl apply -f k8s/redis/secret.yaml
    kubectl apply -f k8s/secrets/create-jwt-secret.yaml
    
    # 데이터베이스 배포
    echo -e "${BLUE}📊 Deploying databases...${NC}"
    kubectl apply -f k8s/mariadb/
    kubectl apply -f k8s/redis/
    
    # NATS JetStream 배포
    echo -e "${BLUE}📡 Deploying NATS JetStream...${NC}"
    kubectl apply -f k8s/nats/
    
    # Istio 설정
    echo -e "${BLUE}🔧 Applying Istio configurations...${NC}"
    kubectl apply -f k8s/istio/
    
    # 애플리케이션 배포
    echo -e "${BLUE}🚀 Deploying application...${NC}"
    kubectl apply -f k8s/signite/
    
    echo -e "${GREEN}✅ K8s environment setup completed${NC}"
    check_status
}

function teardown_k8s() {
    echo -e "${YELLOW}🗑️ Tearing down K8s environment...${NC}"
    
    kubectl delete -f k8s/signite/ --ignore-not-found=true
    kubectl delete -f k8s/istio/ --ignore-not-found=true
    kubectl delete -f k8s/nats/ --ignore-not-found=true
    kubectl delete -f k8s/redis/ --ignore-not-found=true
    kubectl delete -f k8s/mariadb/ --ignore-not-found=true
    
    echo -e "${GREEN}✅ K8s environment teardown completed${NC}"
}

function check_status() {
    echo -e "${YELLOW}📊 Checking K8s deployment status...${NC}"
    echo
    echo -e "${BLUE}📦 Pods:${NC}"
    kubectl get pods -o wide
    echo
    echo -e "${BLUE}🔌 Services:${NC}"
    kubectl get services
    echo
    echo -e "${BLUE}🚀 Deployments:${NC}"
    kubectl get deployments
    echo
}

function view_logs() {
    echo -e "${YELLOW}📋 Viewing application logs...${NC}"
    kubectl logs -f deployment/signite-deployment -c signite-backend
}

function clean_all() {
    echo -e "${YELLOW}🧹 Cleaning all build artifacts...${NC}"
    cd backend
    ./start.sh clean
    cd ..
    
    # Docker 이미지 정리 (선택적)
    read -p "Clean Docker images? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        docker system prune -f
        echo -e "${GREEN}✅ Docker cleanup completed${NC}"
    fi
}

function show_version() {
    echo -e "${BLUE}📋 Version Information:${NC}"
    echo
    
    # Git 정보
    if git rev-parse --git-dir > /dev/null 2>&1; then
        echo -e "${YELLOW}Git Branch:${NC} $(git rev-parse --abbrev-ref HEAD)"
        echo -e "${YELLOW}Git Commit:${NC} $(git rev-parse --short HEAD)"
        echo -e "${YELLOW}Git Status:${NC}"
        git status --porcelain | head -5
    fi
    
    echo
    echo -e "${YELLOW}Environment:${NC}"
    echo -e "  Java: $(java -version 2>&1 | head -n 1)"
    echo -e "  Kotlin: $(kotlinc -version 2>&1 | head -n 1)"
    echo -e "  Docker: $(docker --version)"
    echo -e "  Kubectl: $(kubectl version --client --short 2>/dev/null || echo 'Not installed')"
    echo
}

# WebSocket Functions
function websocket_build() {
    echo -e "${YELLOW}🦀 Building WebSocket server...${NC}"
    cd websocket
    cargo build --release
    cd ..
}

function websocket_docker() {
    echo -e "${YELLOW}🐳 Building WebSocket Docker image...${NC}"
    cd websocket
    
    # Auto-versioning
    VERSION=$(date +%Y%m%d_%H%M%S)
    IMAGE_NAME="signite-websocket"
    
    echo -e "${BLUE}Building ${IMAGE_NAME}:${VERSION}...${NC}"
    docker build -t ${IMAGE_NAME}:${VERSION} .
    docker tag ${IMAGE_NAME}:${VERSION} ${IMAGE_NAME}:latest
    
    # Load to minikube if available
    if command -v minikube &> /dev/null; then
        echo -e "${BLUE}Loading image to minikube...${NC}"
        minikube image load ${IMAGE_NAME}:${VERSION}
        minikube image load ${IMAGE_NAME}:latest
    fi
    
    echo -e "${GREEN}✅ WebSocket image built: ${IMAGE_NAME}:${VERSION}${NC}"
    cd ..
}

function websocket_deploy() {
    echo -e "${YELLOW}🚀 Deploying WebSocket server to K8s...${NC}"
    
    # Apply K8s manifests
    kubectl apply -f k8s/websocket/mongodb.yaml
    kubectl apply -f k8s/websocket/redis.yaml
    kubectl apply -f k8s/websocket/websocket-server.yaml
    kubectl apply -f k8s/websocket/istio.yaml
    
    # Wait for deployment
    echo -e "${BLUE}Waiting for WebSocket deployment...${NC}"
    kubectl rollout status deployment/websocket-server -n default --timeout=300s
    
    echo -e "${GREEN}✅ WebSocket server deployed successfully${NC}"
}

function websocket_dev() {
    echo -e "${YELLOW}🛠️  Starting WebSocket development mode...${NC}"
    
    # Start port-forward for WebSocket
    echo -e "${BLUE}Setting up port forwarding...${NC}"
    kubectl port-forward svc/websocket-service-nodeport 8080:8080 -n default &
    kubectl port-forward svc/websocket-service-nodeport 3001:3001 -n default &
    
    # Show connection info
    echo -e "${GREEN}🔗 WebSocket development URLs:${NC}"
    echo "  WebSocket: ws://localhost:8080"
    echo "  HTTP API:  http://localhost:3001"
    echo "  Health:    http://localhost:3001/health"
    echo
    echo -e "${YELLOW}Press Ctrl+C to stop port forwarding${NC}"
    
    # Start log streaming
    websocket_logs
}

function websocket_logs() {
    echo -e "${YELLOW}📋 Viewing WebSocket server logs...${NC}"
    kubectl logs -f deployment/websocket-server -n default
}

# Full Stack Functions
function all_build() {
    echo -e "${PURPLE}🔨 Building complete Signite stack...${NC}"
    backend_build
    websocket_build
    echo -e "${GREEN}✅ All components built successfully${NC}"
}

function all_docker() {
    echo -e "${PURPLE}🐳 Building all Docker images...${NC}"
    backend_docker
    websocket_docker
    echo -e "${GREEN}✅ All Docker images built successfully${NC}"
}

function all_deploy() {
    echo -e "${PURPLE}🚀 Deploying complete Signite stack...${NC}"
    backend_deploy
    websocket_deploy
    echo -e "${GREEN}✅ Complete stack deployed successfully${NC}"
}

function all_dev() {
    echo -e "${PURPLE}🛠️  Starting full development mode...${NC}"
    
    # Start backend dev mode in background
    echo -e "${BLUE}Starting backend development mode...${NC}"
    backend_dev &
    BACKEND_PID=$!
    
    # Wait a bit for backend to start
    sleep 5
    
    # Start websocket dev mode
    echo -e "${BLUE}Starting WebSocket development mode...${NC}"
    websocket_dev &
    WEBSOCKET_PID=$!
    
    echo -e "${GREEN}🎉 Full development environment is running!${NC}"
    echo -e "${YELLOW}Press Ctrl+C to stop all services${NC}"
    
    # Wait for user interrupt
    trap "kill $BACKEND_PID $WEBSOCKET_PID 2>/dev/null; exit" INT
    wait
}

# 메인 실행 로직
case "${1:-help}" in
    "build")
        backend_build
        ;;
    "test")
        backend_test
        ;;
    "docker")
        backend_docker
        ;;
    "deploy")
        backend_deploy
        ;;
    "dev")
        backend_dev
        ;;
    "ws-build")
        websocket_build
        ;;
    "ws-docker")
        websocket_docker
        ;;
    "ws-deploy")
        websocket_deploy
        ;;
    "ws-dev")
        websocket_dev
        ;;
    "ws-logs")
        websocket_logs
        ;;
    "all-build")
        all_build
        ;;
    "all-docker")
        all_docker
        ;;
    "all-deploy")
        all_deploy
        ;;
    "all-dev")
        all_dev
        ;;
    "setup")
        setup_k8s
        ;;
    "teardown")
        teardown_k8s
        ;;
    "status")
        check_status
        ;;
    "logs")
        view_logs
        ;;
    "clean")
        clean_all
        ;;
    "secrets")
        echo -e "${YELLOW}🔒 Generating secure secrets...${NC}"
        ./scripts/create-secrets.sh
        ;;
    "version")
        show_version
        ;;
    "help"|*)
        show_help
        ;;
esac 