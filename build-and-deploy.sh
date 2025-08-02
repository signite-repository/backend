#!/bin/bash

set -e

echo "=== Signite MSA 서비스 빌드 및 배포 스크립트 ==="

# 1. Category Service 빌드
echo "1. Category Service 빌드 중..."
cd services/category-service
docker build -t signite/category-service:latest .
cd ../..

# 2. Post Service 빌드
echo "2. Post Service 빌드 중..."
cd services/post-service
docker build -t signite/post-service:latest .
cd ../..

# 3. Comment Service 빌드
echo "3. Comment Service 빌드 중..."
cd services/comment-service
docker build -t signite/comment-service:latest .
cd ../..

# 4. WebSocket Service 빌드
echo "4. WebSocket Service 빌드 중..."
cd websocket
docker build -t signite/websocket-service:latest .
cd ..

echo "=== 모든 서비스 이미지 빌드 완료 ==="

# 5. Kubernetes 배포
echo "5. Kubernetes 리소스 배포 중..."

# 데이터베이스 먼저 배포
echo "데이터베이스 배포 중..."
kubectl apply -f k8s/category-db/
kubectl apply -f k8s/post-db/
kubectl apply -f k8s/comment-db/
kubectl apply -f k8s/post-search/

# 서비스 배포
echo "서비스 배포 중..."
kubectl apply -f k8s/category/
kubectl apply -f k8s/category/
kubectl apply -f k8s/post/
kubectl apply -f k8s/comment/
kubectl apply -f k8s/websocket/

# NATS 배포
echo "NATS 배포 중..."
kubectl apply -f k8s/nats/

echo "=== 배포 완료 ==="

echo "배포 상태 확인 중..."
kubectl get pods
kubectl get services

echo "=== 모든 작업 완료 ==="
echo "서비스 상태를 확인하려면: kubectl get pods -w"
echo "로그를 확인하려면: kubectl logs <pod-name>"