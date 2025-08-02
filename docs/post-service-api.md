# 📝 Post Service API 매뉴얼

## 1. 개요

- **담당 서비스**: `post-service`
- **기술 스택**: Kotlin, Spring WebFlux, MariaDB, Elasticsearch
- **주요 기능**:
  - 게시글 CRUD (생성, 조회, 수정, 삭제)
  - Elasticsearch를 이용한 전문(Full-text) 검색
  - 카테고리, 태그와 연동하여 데이터 관리

---

## 2. API 엔드포인트

- **Base URL**: `/api/posts`

### **게시글 목록 조회**
- `GET /?page=0&size=10&sort=createdAt,desc`
- **설명**: 게시글 목록을 페이지네이션하여 조회합니다.
- **인증**: 불필요
- **Response (200 OK)**:
  ```json
  {
    "content": [
      {
        "id": 1,
        "title": "첫 번째 게시글",
        "author": "testuser",
        "categoryName": "공지사항",
        "viewCount": 10,
        "createdAt": "..."
      }
    ],
    "totalPages": 10,
    "totalElements": 100,
    "size": 10,
    "number": 0
  }
  ```

### **게시글 작성**
- `POST /`
- **설명**: 새로운 게시글을 작성합니다.
- **인증**: `SIG_ADMIN` 이상 권한 필요
- **Request Body**:
  ```json
  {
    "title": "새로운 게시글 제목",
    "content": "게시글 내용입니다.",
    "categoryId": 1
  }
  ```
- **Response (201 CREATED)**: 생성된 게시글의 상세 정보 반환

### **게시글 상세 조회**
- `GET /{id}`
- **설명**: 특정 ID의 게시글을 상세 조회합니다.
- **인증**: 불필요
- **Response (200 OK)**:
  ```json
  {
    "id": 1,
    "title": "첫 번째 게시글",
    "content": "게시글의 전체 내용입니다...",
    "author": "testuser",
    "categoryName": "공지사항",
    "tags": ["공지", "중요"],
    "viewCount": 11,
    "createdAt": "...",
    "updatedAt": "..."
  }
  ```

### **게시글 검색**
- `GET /search?query=검색어&page=0&size=10`
- **설명**: Elasticsearch를 통해 제목과 내용에서 검색어를 포함하는 게시글을 검색합니다.
- **인증**: 불필요

---

## 3. 데이터베이스

### **MariaDB**
- **`posts` 테이블**: 게시글의 핵심 메타데이터를 저장합니다.
  | Column          | Type                | 설명                          |
  |-----------------|---------------------|-------------------------------|
  | `id`            | `BIGINT` (PK)       | 게시글 고유 ID                |
  | `title`         | `VARCHAR(255)`      | 제목                          |
  | `content`       | `LONGTEXT`          | 내용 (별도 테이블 분리 가능)   |
  | `user_id`       | `BIGINT`            | 작성자 ID                     |
  | `category_id`   | `BIGINT`            | 카테고리 ID                   |
  | `organization_id`| `BIGINT`           | 소속 조직 ID                  |
  | `status`        | `ENUM`              | 게시글 상태 (공개, 임시 등)   |
  | `view_count`    | `BIGINT`            | 조회수                        |

### **Elasticsearch**
- **인덱스**: `posts`
- **문서 필드**:
  - `title`: 텍스트 검색을 위한 `text` 타입 (nori 분석기 사용)
  - `content`: 텍스트 검색을 위한 `text` 타입 (nori 분석기 사용)
  - `tags`: 키워드 검색을 위한 `keyword` 타입
  - `createdAt`: 날짜 필터링을 위한 `date` 타입
- **동기화**: MariaDB의 `posts` 테이블에 변경이 발생하면, 이벤트를 통해 Elasticsearch 인덱스가 업데이트됩니다.
