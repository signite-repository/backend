#!/bin/bash

echo "=== 전체 서비스 테스트 시작 ==="

# 1. Pod 상태 확인
echo ""
echo "1. Pod 상태 확인"
echo "================================="
kubectl get pods --all-namespaces | grep -E "(category|auth|redis|monitoring|istio)" | grep -v Terminating

# 2. Category Service 테스트
echo ""
echo ""
echo "2. Category Service 테스트"
echo "================================="
echo "Health Check:"
curl -s http://localhost:8081/actuator/health | head -100
echo ""
echo "API Test - GET /api/v1/categories:"
curl -s http://localhost:8081/api/v1/categories | head -100

# 3. Auth Service 테스트
echo ""
echo ""
echo "3. Auth Service 테스트"
echo "================================="
echo "Health Check:"
curl -s http://localhost:8082/actuator/health || echo "Auth Service not responding"
echo ""
echo "API Test - POST /api/auth/register:"
curl -X POST http://localhost:8082/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@signight.com","password":"Test1234!"}' \
  -s | head -100 || echo "Auth registration API not working"

# 4. Prometheus 테스트
echo ""
echo ""
echo "4. Prometheus 테스트"
echo "================================="
curl -s http://localhost:9090/api/v1/targets | grep -o '"health":"[^"]*"' | head -5 || echo "Prometheus not responding"

# 5. Grafana 테스트
echo ""
echo ""
echo "5. Grafana 테스트"
echo "================================="
curl -s http://localhost:3000/api/health || echo "Grafana not responding"

# 6. Service 목록
echo ""
echo ""
echo "6. Service 목록"
echo "================================="
kubectl get svc --all-namespaces | grep -E "(category|auth|redis|prometheus|grafana|istio)"

echo ""
echo "=== 테스트 완료 ===
"