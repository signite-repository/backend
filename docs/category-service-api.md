# ✅ Category Service API 현행화 가이드

## 1. 개요

- **담당 서비스**: `category-service`
- **기술 스택**: Spring WebFlux, Kotlin, MongoDB
- **주요 기능**: 계층형 카테고리 데이터를 관리하며, Tree 구조로 조회하는 API를 제공합니다.

## 2. API 엔드포인트

Istio 게이트웨이를 통해 라우팅되며, 외부에서 호출하는 최종 엔드포인트 기준입니다.

### **카테고리 전체 조회 (Tree 구조)**
- `GET /api/categories`
- **설명**: 모든 카테고리를 부모-자식 관계가 포함된 트리 구조로 조회합니다.
- **인증**: 불필요
- **응답 (200 OK)**:
  ```json
  [
    {
      "id": "1",
      "name": "개발",
      "slug": "development",
      "children": [
        {
          "id": "2",
          "name": "프론트엔드",
          "slug": "frontend",
          "children": []
        }
      ]
    }
  ]
  ```

### **특정 카테고리 조회**
- `GET /api/categories/{id}`
- **설명**: 지정된 `id`에 해당하는 카테고리 정보를 조회합니다.
- **인증**: 불필요
- **응답 (200 OK)**:
  ```json
  {
    "id": "1",
    "name": "개발",
    "slug": "development",
    "parentId": null,
    "path": "development"
  }
  ```

## 3. 데이터 모델 (MongoDB)

- **컬렉션 이름**: `categories`
- **핵심 필드**:
  - `name`: 카테고리 이름 (String)
  - `slug`: URL 식별자 (String, Unique)
  - `parentId`: 부모 ID (String, 최상위는 null)
  - `path`: 전체 경로 (String, e.g., "dev/backend")
  - `level`: 깊이 (Int)
  - `metadata`: 아이콘, 색상 등 추가 정보 (Map)

## 4. Kubernetes 주요 설정

- **데이터베이스**: MongoDB (`StatefulSet`으로 배포)
- **네트워크 정책**: `category-service` 파드만 MongoDB에 접근 가능하도록 제한됩니다.
- **헬스 체크**: `/actuator/health/liveness`, `/actuator/health/readiness`
