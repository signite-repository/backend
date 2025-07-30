#!/bin/bash

# 🚀 Signite Backend Development Script
# K8s + Istio 기반 현대적 개발 워크플로우

set -e

export JAVA_HOME="/c/Program Files/Java/jdk-23"
export PATH="$JAVA_HOME/bin:$PATH"

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 현재 Git 브랜치와 커밋 해시로 자동 버전 생성
BRANCH=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "main")
COMMIT=$(git rev-parse --short HEAD 2>/dev/null || echo "latest")
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
VERSION="${BRANCH}-${COMMIT}-${TIMESTAMP}"

IMAGE_NAME="signite-backend"
REGISTRY="localhost:5000"  # 로컬 레지스트리 (필요시 변경)

echo -e "${BLUE}🚀 Signite Backend Development Workflow${NC}"
echo -e "${YELLOW}📋 Auto-generated version: ${VERSION}${NC}"
echo

function show_help() {
    echo "Usage: ./start.sh [COMMAND]"
    echo
    echo "Commands:"
    echo "  build     - Build application only"
    echo "  docker    - Build Docker image with auto-versioning"
    echo "  deploy    - Build & Deploy to K8s cluster"
    echo "  dev       - Start development mode (port-forward + logs)"
    echo "  clean     - Clean build artifacts"
    echo "  test      - Run tests"
    echo "  help      - Show this help"
    echo
}

function clean_build() {
    echo -e "${YELLOW}🧹 Cleaning build directory...${NC}"
    rm -rf ./build
    ./gradlew clean
}

function build_app() {
    echo -e "${YELLOW}🔨 Building application with Java...${NC}"
    java -version
    ./gradlew build -x test
    echo -e "${GREEN}✅ Build completed${NC}"
}

function run_tests() {
    echo -e "${YELLOW}🧪 Running tests...${NC}"
    ./gradlew test
    echo -e "${GREEN}✅ Tests completed${NC}"
}

function build_docker() {
    echo -e "${YELLOW}🐳 Building Docker image: ${IMAGE_NAME}:${VERSION}${NC}"
    
    # Dockerfile에 버전 정보 주입
    docker build \
        --build-arg VERSION=${VERSION} \
        --build-arg BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ') \
        --build-arg GIT_COMMIT=${COMMIT} \
        -t ${IMAGE_NAME}:${VERSION} \
        -t ${IMAGE_NAME}:latest \
        .
    
    echo -e "${GREEN}✅ Docker image built: ${IMAGE_NAME}:${VERSION}${NC}"
}

function deploy_k8s() {
    echo -e "${YELLOW}🚢 Deploying to Kubernetes...${NC}"
    
    # K8s 배포 파일에서 이미지 태그 업데이트
    sed -i.bak "s|signite-backend:.*|signite-backend:${VERSION}|g" ../k8s/signite/deployment.yaml
    
    # K8s 배포 실행
    kubectl apply -f ../k8s/signite/
    
    echo -e "${GREEN}✅ Deployed to K8s with version: ${VERSION}${NC}"
    
    # 배포 상태 확인
    echo -e "${YELLOW}📊 Checking deployment status...${NC}"
    kubectl rollout status deployment/signite-deployment
}

function start_dev_mode() {
    echo -e "${YELLOW}💻 Starting development mode...${NC}"
    
    # 포트포워딩 시작 (백그라운드)
    echo -e "${BLUE}🔌 Setting up port forwarding...${NC}"
    kubectl port-forward svc/signite-service 8080:8080 &
    PORT_FORWARD_PID=$!
    
    # 잠시 대기
    sleep 3
    
    echo -e "${GREEN}✅ Port forwarding active: http://localhost:8080${NC}"
    echo -e "${BLUE}📡 Following application logs...${NC}"
    echo -e "${RED}Press Ctrl+C to stop dev mode${NC}"
    echo
    
    # 로그 실시간 확인
    trap "kill $PORT_FORWARD_PID 2>/dev/null; exit" INT
    kubectl logs -f deployment/signite-deployment -c signite-backend
}

# 메인 실행 로직
case "${1:-help}" in
    "build")
        build_app
        ;;
    "docker")
        build_app
        build_docker
        ;;
    "deploy")
        build_app
        build_docker
        deploy_k8s
        ;;
    "dev")
        start_dev_mode
        ;;
    "clean")
        clean_build
        ;;
    "test")
        run_tests
        ;;
    "help"|*)
        show_help
        ;;
esac