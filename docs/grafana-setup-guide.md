# Grafana 대시보드 설정 가이드

## 1. Grafana 접속 및 초기 설정

### 접속 정보
- **URL**: http://localhost:3000
- **Username**: admin
- **Password**: admin123

### 첫 접속 후 설정
1. 브라우저에서 http://localhost:3000 접속
2. admin/admin123로 로그인
3. 필요시 비밀번호 변경

## 2. Prometheus 데이터 소스 추가

### 단계별 설정
1. **Configuration (⚙️) → Data Sources** 클릭
2. **Add data source** 클릭
3. **Prometheus** 선택
4. 다음 정보 입력:
   - **Name**: `prometheus`
   - **URL**: `http://prometheus-service.monitoring.svc.cluster.local:9090`
   - **Access**: `Server (default)`
5. **Save & test** 클릭하여 연결 확인

## 3. 대시보드 임포트

### 방법 1: JSON 파일 임포트
1. **+ (Create) → Import** 클릭
2. **Upload JSON file** 또는 **Import via panel json** 선택
3. 제공된 JSON 파일 업로드:
   - `grafana-dashboard-category-service.json` - Category Service 전용
   - `grafana-dashboard-infrastructure.json` - 인프라 모니터링

### 방법 2: JSON 텍스트 직접 붙여넣기
1. **+ (Create) → Import** 클릭
2. **Import via panel json** 선택
3. JSON 내용 전체를 복사하여 붙여넣기
4. **Load** 클릭
5. **Import** 클릭

## 4. 대시보드 구성

### Category Service Dashboard
포함된 패널:
- **HTTP Requests Rate**: API 요청 비율
- **JVM Memory Usage**: 자바 메모리 사용률
- **HTTP Response Time**: API 응답 시간 (95th, 50th percentile)
- **Service Health**: 서비스 상태
- **MongoDB Connections**: MongoDB 연결 상태
- **MongoDB Operations**: MongoDB 작업 통계
- **MongoDB Memory Usage**: MongoDB 메모리 사용량
- **Pod Status**: Kubernetes 파드 상태
- **MongoDB Document Operations**: 문서 작업 통계

### Infrastructure Overview Dashboard
포함된 패널:
- **Node Memory/CPU/Disk Usage**: 노드 리소스 사용률
- **Ready Pods**: 준비된 파드 수
- **Container Memory/CPU Usage**: 컨테이너 리소스 사용량
- **Network I/O**: 네트워크 입출력
- **Container Restarts**: 컨테이너 재시작 통계

## 5. 알림 설정 (선택사항)

### 알림 채널 설정
1. **Alerting → Notification channels** 이동
2. **Add channel** 클릭
3. 원하는 알림 방식 선택 (Slack, Email, Webhook 등)

### 알림 규칙 예제
```json
{
  "alert": {
    "conditions": [
      {
        "query": {
          "queryType": "",
          "refId": "A"
        },
        "reducer": {
          "type": "last",
          "params": []
        },
        "evaluator": {
          "params": [80],
          "type": "gt"
        }
      }
    ],
    "executionErrorState": "alerting",
    "for": "5m",
    "frequency": "10s",
    "handler": 1,
    "name": "High Memory Usage Alert",
    "noDataState": "no_data"
  }
}
```

## 6. 커스터마이징

### 패널 편집
1. 패널 제목 클릭 → **Edit** 선택
2. 쿼리 수정, 시각화 옵션 변경
3. **Apply** 클릭하여 저장

### 새 패널 추가
1. 대시보드에서 **Add panel** 클릭
2. **Add a new panel** 선택
3. Prometheus 쿼리 작성
4. 시각화 타입 선택 (Graph, Stat, Gauge 등)

## 7. 유용한 Prometheus 쿼리

### Category Service 관련
```promql
# HTTP 요청 비율
rate(http_server_requests_seconds_count{application="category-service"}[5m])

# JVM 메모리 사용률
jvm_memory_used_bytes{application="category-service"} / jvm_memory_max_bytes{application="category-service"} * 100

# HTTP 응답 시간 95퍼센타일
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket{application="category-service"}[5m]))
```

### MongoDB 관련
```promql
# MongoDB 연결 수
mongodb_connections{state="current"}

# MongoDB 작업 비율
rate(mongodb_op_counters_total[5m])

# MongoDB 메모리 사용량
mongodb_memory{type="resident"}
```

### Kubernetes 관련
```promql
# 파드 준비 상태
kube_pod_status_ready{namespace="default"}

# 컨테이너 메모리 사용량
container_memory_usage_bytes{pod=~"category-.*"}

# 컨테이너 CPU 사용률
rate(container_cpu_usage_seconds_total{pod=~"category-.*"}[5m])
```

## 8. 트러블슈팅

### 데이터가 표시되지 않는 경우
1. **Prometheus 연결 확인**: Data Sources에서 Test 버튼 클릭
2. **메트릭 확인**: Prometheus UI에서 메트릭 존재 여부 확인
3. **시간 범위 확인**: 대시보드 상단 시간 선택기 확인
4. **쿼리 검증**: Panel Edit에서 쿼리 문법 확인

### 성능 최적화
1. **Refresh 간격 조정**: 기본 30초에서 필요에 따라 조정
2. **시간 범위 제한**: 너무 긴 시간 범위는 성능 저하 유발
3. **쿼리 최적화**: 복잡한 쿼리는 간소화

이제 Grafana에서 Category Service와 인프라를 종합적으로 모니터링할 수 있습니다!