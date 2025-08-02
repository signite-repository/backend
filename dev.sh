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
    echo -e "${YELLOW}Development Commands:${NC}"
    echo "  dev         - Start unified development mode via Istio Gateway"
    echo "  logs        - View all application logs"
    echo
    echo -e "${YELLOW}Build & Deploy Commands:${NC}"
    echo "  build       - Build all services"
    echo "  docker      - Build all Docker images"
    echo "  deploy      - Deploy complete stack to K8s"
    echo
    echo -e "${YELLOW}Infrastructure Commands:${NC}"
    echo "  setup       - Setup complete K8s environment"
    echo "  teardown    - Teardown K8s environment"
    echo "  status      - Check K8s deployment status"
    echo
    echo -e "${YELLOW}Utility Commands:${NC}"
    echo "  clean       - Clean all build artifacts"
    echo "  version     - Show version information"
    echo "  help        - Show this help"
    echo
}

# --- Build and Deploy Functions ---

function build_all() {
    echo -e "${PURPLE}🔨 Building complete Signite stack...${NC}"
    (cd services/auth-service && ./gradlew build)
    (cd services/category-service && ./gradlew build)
    (cd services/post-service && ./gradlew build)
    (cd services/comment-service && cargo build --release)
    (cd websocket && cargo build --release)
    echo -e "${GREEN}✅ All components built successfully${NC}"
}

function docker_all() {
    echo -e "${PURPLE}🐳 Building all Docker images...${NC}"
    
    # 이 부분은 각 서비스의 Docker 빌드 스크립트나 로직을 호출해야 합니다.
    # 예시로 각 디렉토리에서 docker build를 실행하는 형태로 남겨둡니다.
    docker build -t signite/auth-service:latest services/auth-service
    docker build -t signite/category-service:latest services/category-service
    docker build -t signite/post-service:latest services/post-service
    docker build -t signite/comment-service:latest services/comment-service
    docker build -t signite/websocket:latest websocket
    
    echo -e "${GREEN}✅ All Docker images built successfully${NC}"
}

function deploy_all() {
    echo -e "${PURPLE}🚀 Deploying complete Signite stack...${NC}"
    kubectl apply -k k8s/
    
    echo -e "${BLUE}Waiting for deployments to be ready...${NC}"
    # kubectl wait --for=condition=available --timeout=5m deployment -l app.kubernetes.io/part-of=signite
    
    echo -e "${GREEN}✅ Complete stack deployed successfully${NC}"
    check_status
}


# --- Development Functions ---

function start_dev_mode() {
    echo -e "${PURPLE}🛠️  Starting unified development mode via Istio Gateway...${NC}"

    echo -e "${BLUE}Setting up single entrypoint port-forward...${NC}"
    
    # istio-ingressgateway의 Pod 이름을 가져옵니다.
    INGRESS_POD=$(kubectl get pod -l istio=ingressgateway -n istio-system -o jsonpath='{.items[0].metadata.name}')
    if [ -z "$INGRESS_POD" ]; then
        echo -e "${RED}Error: istio-ingressgateway pod not found in istio-system namespace.${NC}"
        exit 1
    fi
    
    # 게이트웨이 포트 포워딩을 백그라운드에서 실행
    kubectl port-forward -n istio-system pod/$INGRESS_POD 8080:80 &
    PORT_FORWARD_PID=$!
    
    # 스크립트 종료 시 포트 포워딩 프로세스도 함께 종료되도록 설정
    trap "echo -e '\n${YELLOW}Stopping port-forward...${NC}'; kill $PORT_FORWARD_PID 2>/dev/null" EXIT
    
    echo
    echo -e "${GREEN}✅ Development environment is running!${NC}"
    echo -e "All services are accessible through the gateway at ${YELLOW}http://localhost:8080${NC}"
    echo
    echo -e "${BLUE}Example endpoints:${NC}"
    echo -e "  Auth Service:      ${YELLOW}http://localhost:8080/api/auth/...${NC}"
    echo -e "  Post Service:      ${YELLOW}http://localhost:8080/api/posts/...${NC}"
    echo -e "  Category Service:  ${YELLOW}http://localhost:8080/api/categories/...${NC}"
    echo -e "  WebSocket:         ${YELLOW}ws://localhost:8080/api/ws${NC}"
    echo

    # 통합 로그 뷰어 실행
    view_logs
}

function view_logs() {
    echo -e "${YELLOW}📋 Viewing all application logs... (Press Ctrl+C to stop)${NC}"
    
    # stern이 설치되어 있는지 확인
    if ! command -v stern &> /dev/null; then
        echo -e "${RED}Warning: 'stern' is not installed. Displaying logs with 'kubectl'.${NC}"
        echo -e "${BLUE}For a better experience, install stern: https://github.com/stern/stern${NC}"
        
        # kubectl을 사용한 대체 로깅
        # 이 부분은 프로젝트의 모든 서비스 Pod를 선택할 수 있는 공통 레이블을 사용해야 합니다.
        # 예: app.kubernetes.io/part-of=signite
                         kubectl logs -f -l 'app in (auth-service, category-service, comment-service, post-service, websocket-server)' --all-containers --max-log-requests=30
    else
        # stern을 사용한 통합 로그
        stern . -n default --exclude-container istio-proxy
    fi
}


# --- Infrastructure & Utility Functions ---

function setup_k8s() {
    echo -e "${YELLOW}🏗️ Setting up complete K8s environment...${NC}"
    echo -e "${BLUE}Applying all configurations from k8s/ directory...${NC}"
    # kubectl apply -k k8s/
    echo -e "${GREEN}✅ K8s environment setup completed${NC}"
    check_status
}

function teardown_k8s() {
    echo -e "${YELLOW}🗑️ Tearing down K8s environment...${NC}"
    # kubectl delete -k k8s/ --ignore-not-found=true
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

function clean_all() {
    echo -e "${YELLOW}🧹 Cleaning all build artifacts...${NC}"
    (cd services/auth && ./gradlew clean)
    (cd services/category && ./gradlew clean)
    (cd services/post && ./gradlew clean)
    (cd services/comment && cargo clean)
    (cd services/websocket && cargo clean)
    
    read -p "Clean Docker images? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        # 주의: 이 명령은 현재 빌드되지 않은 이미지도 삭제할 수 있습니다.
        docker image prune -a -f
        echo -e "${GREEN}✅ Docker image cleanup completed${NC}"
    fi
}

function show_version() {
    echo -e "${BLUE}📋 Version Information:${NC}"
    echo
    
    if git rev-parse --git-dir > /dev/null 2>&1; then
        echo -e "${YELLOW}Git Branch:${NC} $(git rev-parse --abbrev-ref HEAD)"
        echo -e "${YELLOW}Git Commit:${NC} $(git rev-parse --short HEAD)"
    fi
    
    echo
    echo -e "${YELLOW}Environment:${NC}"
    echo -e "  Docker: $(docker --version)"
    echo -e "  Kubectl: $(kubectl version --client --short 2>/dev/null || echo 'Not installed')"
    echo
}

# --- Main Execution Logic ---

case "${1:-help}" in
    "dev")
        start_dev_mode
        ;;
    "logs")
        view_logs
        ;;
    "build")
        build_all
        ;;
    "docker")
        docker_all
        ;;
    "deploy")
        deploy_all
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
    "clean")
        clean_all
        ;;
    "version")
        show_version
        ;;
    "help"|*)
        show_help
        ;;
esac
