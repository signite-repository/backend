#!/bin/bash

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🚀 WebSocket 서버 배포 스크립트${NC}"
echo "=================================="

# 변수 설정
IMAGE_NAME="websocket-server"
IMAGE_TAG="latest"
NAMESPACE="websocket"

# Docker 빌드
echo -e "${YELLOW}📦 Docker 이미지 빌드 중...${NC}"
docker build -t ${IMAGE_NAME}:${IMAGE_TAG} .

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Docker 이미지 빌드 완료${NC}"
else
    echo -e "${RED}❌ Docker 이미지 빌드 실패${NC}"
    exit 1
fi

# Docker 이미지를 minikube에 로드 (로컬 개발 환경용)
if command -v minikube &> /dev/null; then
    echo -e "${YELLOW}📤 Minikube에 이미지 로드 중...${NC}"
    minikube image load ${IMAGE_NAME}:${IMAGE_TAG}
    echo -e "${GREEN}✅ Minikube 이미지 로드 완료${NC}"
fi

# 네임스페이스 생성
echo -e "${YELLOW}🏗️  네임스페이스 생성 중...${NC}"
kubectl apply -f ../k8s/rust-websocket/namespace.yaml

# MongoDB 배포
echo -e "${YELLOW}🍃 MongoDB 배포 중...${NC}"
kubectl apply -f ../k8s/rust-websocket/mongodb.yaml

# Redis 배포
echo -e "${YELLOW}🔴 Redis 배포 중...${NC}"
kubectl apply -f ../k8s/rust-websocket/redis.yaml

# WebSocket 서버 배포
echo -e "${YELLOW}🌐 WebSocket 서버 배포 중...${NC}"
kubectl apply -f ../k8s/rust-websocket/websocket-server.yaml

# Istio 설정 적용
echo -e "${YELLOW}🕸️  Istio 설정 적용 중...${NC}"
kubectl apply -f ../k8s/rust-websocket/istio.yaml

# 배포 상태 확인
echo -e "${YELLOW}⏳ 배포 상태 확인 중...${NC}"
kubectl rollout status deployment/websocket-server -n ${NAMESPACE} --timeout=300s

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ WebSocket 서버 배포 완료${NC}"
else
    echo -e "${RED}❌ WebSocket 서버 배포 실패${NC}"
    exit 1
fi

# 서비스 정보 출력
echo -e "${BLUE}📋 배포된 서비스 정보${NC}"
echo "=================================="
kubectl get all -n ${NAMESPACE}

echo ""
echo -e "${BLUE}🔗 연결 정보${NC}"
echo "=================================="

# NodePort 서비스 정보
WEBSOCKET_NODEPORT=$(kubectl get svc websocket-service-nodeport -n ${NAMESPACE} -o jsonpath='{.spec.ports[?(@.name=="websocket")].nodePort}')
HTTP_NODEPORT=$(kubectl get svc websocket-service-nodeport -n ${NAMESPACE} -o jsonpath='{.spec.ports[?(@.name=="http")].nodePort}')

if command -v minikube &> /dev/null; then
    MINIKUBE_IP=$(minikube ip)
    echo -e "WebSocket URL: ${GREEN}ws://${MINIKUBE_IP}:${WEBSOCKET_NODEPORT}${NC}"
    echo -e "HTTP API URL: ${GREEN}http://${MINIKUBE_IP}:${HTTP_NODEPORT}${NC}"
    echo -e "Health Check: ${GREEN}http://${MINIKUBE_IP}:${HTTP_NODEPORT}/health${NC}"
else
    echo -e "WebSocket Port: ${GREEN}${WEBSOCKET_NODEPORT}${NC}"
    echo -e "HTTP API Port: ${GREEN}${HTTP_NODEPORT}${NC}"
fi

echo ""
echo -e "${GREEN}🎉 배포가 성공적으로 완료되었습니다!${NC}" 