# Category Service API Documentation

## Overview
Category Service는 다중 깊이 트리 구조의 카테고리 관리를 제공하는 마이크로서비스입니다.

## 기술 스택
- **Language**: Kotlin
- **Framework**: Spring Boot 3.x, Spring WebFlux
- **Database**: MongoDB
- **Container**: Docker, Kubernetes

## API Endpoints

### 1. 전체 카테고리 트리 조회
```
GET /api/v1/categories
```

**Response**:
```json
[
  {
    "id": "1",
    "name": "개발",
    "slug": "development",
    "parentId": null,
    "path": "development",
    "level": 0,
    "displayOrder": 1,
    "metadata": {
      "icon": "code",
      "color": "#2196F3"
    },
    "children": [
      {
        "id": "2",
        "name": "프론트엔드",
        "slug": "frontend",
        "parentId": "1",
        "path": "development/frontend",
        "level": 1,
        "displayOrder": 1,
        "metadata": {},
        "children": []
      }
    ]
  }
]
```

### 2. 슬러그로 카테고리 조회
```
GET /api/v1/categories/slug/{slug}
```

**Parameters**:
- `slug`: 카테고리 슬러그 (예: "development")

**Response**:
```json
{
  "id": "1",
  "name": "개발",
  "slug": "development",
  "parentId": null,
  "path": "development",
  "level": 0,
  "displayOrder": 1,
  "metadata": {
    "icon": "code",
    "color": "#2196F3"
  }
}
```

## 데이터 모델

### Category Document
```kotlin
@Document("categories")
data class Category(
    @Id
    val id: String? = null,
    var name: String,
    var slug: String,
    var parentId: String?,
    var path: String,
    var level: Int,
    var displayOrder: Int,
    var metadata: Map<String, Any>? = emptyMap(),
    val createdAt: LocalDateTime? = LocalDateTime.now()
)
```

### 필드 설명
- `id`: MongoDB ObjectId
- `name`: 카테고리 표시 이름
- `slug`: URL-friendly 고유 식별자
- `parentId`: 부모 카테고리 ID (최상위 카테고리는 null)
- `path`: 계층 경로 (예: "root/sub1/sub2")
- `level`: 트리 깊이 (0부터 시작)
- `displayOrder`: 같은 레벨에서의 표시 순서
- `metadata`: 추가 정보를 저장하는 맵 (아이콘, 색상 등)

## Kubernetes 구성

### StatefulSet
MongoDB는 StatefulSet으로 배포되어 영구 스토리지를 보장합니다.
```yaml
- PVC: 1Gi 스토리지
- Health checks 구성
- Resource limits 설정
```

### HorizontalPodAutoscaler
Category Service는 HPA로 자동 스케일링됩니다.
```yaml
- Min replicas: 2
- Max replicas: 10
- CPU target: 70%
- Memory target: 80%
```

### NetworkPolicy
보안을 위해 네트워크 정책이 적용됩니다.
- Category Service만 MongoDB에 접근 가능
- 외부에서는 8080 포트로만 접근 가능

## 환경 설정

### MongoDB 연결
```yaml
spring:
  data:
    mongodb:
      uri: ${MONGO_URI}
      database: categorydb
```

### Health Checks
- Liveness: `/actuator/health/liveness`
- Readiness: `/actuator/health/readiness`

## 테스트

### Unit Tests
- CategoryServiceTest
- CategoryRepositoryTest
- CategoryResourceTest

### Test Coverage
- Service layer: 비즈니스 로직 테스트
- Repository layer: MongoDB 연동 테스트
- Controller layer: API 엔드포인트 테스트

## 보안
- MongoDB 인증: Secret으로 관리
- 네트워크 격리: NetworkPolicy 적용
- Resource limits: 리소스 제한 설정