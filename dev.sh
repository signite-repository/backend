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