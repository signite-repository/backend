# Istio와 Kubernetes 네이티브 인증 통합 가이드

## 개요

이 문서는 Category Service에 Istio 기반 인증 및 RBㅣㅣAC(Role-Based Access Control)을 설정하는 방법을 설명합니다. [[memory:4642952]]에 따라 이 프로젝트는 이미 Istio 기반 로그인/로그아웃을 사용하므로, 애플리케이션 레벨 인증이 필요하지 않습니다.

## 1. 구성 요소
ㅣㅣ
### 1.1 생성된 파일들
- `k8s/category-service/istio.yaml` - Istio Gateway, VirtualService, AuthorizationPolicy
- `k8s/category-service/rbac.yaml` - Kubernetes RBAC 설정
- `k8s/istio/k8s-auth-integration.yaml` - K8s 토큰과 Istio 통합

### 1.2 주요 기능
- **mTLS**: 서비스 간 통신 암호화
- **JWT 인증**: Kubernetes ServiceAccount 토큰 활용
- **RBAC**: 역할 기반 접근 제어
- **자동 사이드카 주입**: Istio 프록시 자동 설정

## 2. 인증 흐름

### 2.1 외부 요청
1. 클라이언트가 K8s ServiceAccount 토큰을 Bearer 토큰으로 전송
2. Istio가 토큰을 검증 (JWKS 엔드포인트 사용)
3. AuthorizationPolicy가 claims 기반으로 접근 권한 확인
4. 허용된 요청만 서비스로 전달

### 2.2 내부 서비스 간 통신
1. mTLS로 자동 인증
2. ServiceAccount 기반 인증
3. 네임스페이스 기반 격리

## 3. 설정 적용

### 3.1 Istio 설치 확인
```bash
# Istio 설치 상태 확인
kubectl get namespace istio-system
kubectl get pods -n istio-system

# 없다면 설치
istioctl install --set profile=demo -y
```

### 3.2 RBAC 및 ServiceAccount 적용
```bash
# RBAC 설정 적용
kubectl apply -f k8s/category-service/rbac.yaml

# 기존 deployment 업데이트 (ServiceAccount 추가됨)
kubectl apply -f k8s/category-service/deployment.yaml
```

### 3.3 Istio 설정 적용
```bash
# Istio Gateway, VirtualService, AuthorizationPolicy 적용
kubectl apply -f k8s/category-service/istio.yaml

# K8s 인증 통합 설정
kubectl apply -f k8s/istio/k8s-auth-integration.yaml
```

### 3.4 자동 사이드카 주입 활성화
```bash
# default 네임스페이스에 자동 주입 레이블 추가
kubectl label namespace default istio-injection=enabled

# Pod 재시작하여 사이드카 주입
kubectl rollout restart deployment/category-service-deployment
```

## 4. 테스트

### 4.1 ServiceAccount 토큰 생성
```bash
# ServiceAccount 토큰 생성 (1시간 유효)
TOKEN=$(kubectl create token category-service-sa --duration=1h)

# 또는 사용자 토큰 사용
TOKEN=$(kubectl config view --raw -o jsonpath='{.users[0].user.token}')
```

### 4.2 인증된 요청 테스트
```bash
# 헬스체크 (인증 불필요)
curl http://category.signight.com/actuator/health

# 카테고리 조회 (인증 필요)
curl -H "Authorization: Bearer $TOKEN" \
     http://category.signight.com/api/v1/categories

# 카테고리 생성 (admin 권한 필요)
curl -X POST \
     -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"name":"테스트","slug":"test"}' \
     http://category.signight.com/api/v1/categories
```

### 4.3 Istio 프록시 로그 확인
```bash
# 사이드카 프록시 로그
kubectl logs category-service-deployment-xxx -c istio-proxy

# 인증 정보 확인
kubectl exec category-service-deployment-xxx -c istio-proxy -- \
  curl -s localhost:15000/config_dump | grep jwt
```

## 5. 권한 관리

### 5.1 역할 정의
- **admin**: 모든 CRUD 작업 가능
- **category-admin**: 카테고리 관련 모든 작업
- **viewer**: 읽기 전용
- **service**: 서비스 간 통신용

### 5.2 사용자/그룹 매핑
```yaml
# k8s-auth-mapping ConfigMap에서 관리
groups:
  system:masters: admin
  category-admins: category-admin
  developers: viewer
users:
  ydh2244: admin
```

### 5.3 권한 부여 예제
```bash
# 사용자를 admin 그룹에 추가
kubectl create clusterrolebinding ydh2244-admin \
  --clusterrole=category-admin \
  --user=ydh2244

# ServiceAccount에 권한 부여
kubectl create rolebinding post-service-category-reader \
  --clusterrole=category-viewer \
  --serviceaccount=default:post-service \
  -n default
```

## 6. 모니터링

### 6.1 Kiali 대시보드
```bash
# Kiali 설치 (Istio addon)
kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-1.20/samples/addons/kiali.yaml

# 포트 포워딩
kubectl port-forward -n istio-system svc/kiali 20001:20001
```

### 6.2 인증 메트릭
- Prometheus에서 `istio_request_duration_milliseconds` 확인
- 401/403 응답 비율 모니터링
- JWT 검증 실패율 추적

## 7. 트러블슈팅

### 7.1 401 Unauthorized
- 토큰 만료 확인: `kubectl create token` 재실행
- 토큰 형식 확인: Bearer 접두사 필요
- Issuer 확인: `https://kubernetes.default.svc.cluster.local`

### 7.2 403 Forbidden  
- 사용자 그룹 확인: `kubectl auth can-i`
- AuthorizationPolicy rules 검토
- ServiceAccount 권한 확인

### 7.3 사이드카 문제
- Pod에 istio-proxy 컨테이너 확인
- `istio-injection=enabled` 레이블 확인
- Pod 재시작 필요

## 8. 보안 고려사항

### 8.1 프로덕션 권장사항
- mTLS를 STRICT 모드로 설정 ✓
- 최소 권한 원칙 적용 ✓
- 토큰 수명 제한 (1시간 이하)
- 정기적인 권한 검토

### 8.2 추가 보안 강화
- NetworkPolicy로 네트워크 격리
- PodSecurityPolicy 적용
- 감사 로깅 활성화

이제 Category Service는 Kubernetes 네이티브 인증과 Istio RBAC으로 완전히 보호됩니다!