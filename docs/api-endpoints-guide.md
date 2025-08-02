# 🚀 Signite 프론트엔드 API 연동 가이드

## 1. 기본 URL

모든 API 요청은 아래 기본 URL을 시작점으로 사용합니다. 로컬 개발 환경에서는 별도의 포트 번호 없이 Istio Gateway를 통해 각 서비스로 라우팅됩니다.

- **HTTP API Base URL**: `http://localhost`
- **WebSocket URL**: `ws://localhost`

---

## 2. 도메인별 API 요청 주소

### 🔐 인증 (Auth Service)

| 기능       | Method | 전체 요청 주소 (Full URL)            | 인증   |
|------------|--------|--------------------------------------|--------|
| **로그인**     | `POST` | `http://localhost/api/auth/login`    | 불필요 |
| **회원가입**   | `POST` | `http://localhost/api/auth/register` | 불필요 |
| **토큰 갱신**  | `POST` | `http://localhost/api/auth/refresh`  | 필요   |
| **내 정보 조회**| `GET`  | `http://localhost/api/auth/me`       | 필요   |

### 📂 카테고리 (Category Service)

| 기능             | Method | 전체 요청 주소 (Full URL)                 | 인증   |
|------------------|--------|-------------------------------------------|--------|
| **전체 카테고리 조회** | `GET`  | `http://localhost/api/categories`         | 불필요 |
| **특정 카테고리 조회**| `GET`  | `http://localhost/api/categories/{id}`    | 불필요 |
| **카테고리 생성**    | `POST` | `http://localhost/api/categories`         | ADMIN  |

### 📝 게시글 (Post Service)

| 기능             | Method | 전체 요청 주소 (Full URL)                       | 인증        |
|------------------|--------|-------------------------------------------------|-------------|
| **게시글 목록 조회** | `GET`  | `http://localhost/api/posts?page=0&size=10`     | 불필요      |
| **게시글 상세 조회** | `GET`  | `http://localhost/api/posts/{id}`               | 불필요      |
| **게시글 작성**      | `POST` | `http://localhost/api/posts`                    | SIG_ADMIN   |
| **게시글 검색**      | `GET`  | `http://localhost/api/posts/search?query=...`   | 불필요      |

### 💬 댓글 (Comment Service)

| 기능             | Method | 전체 요청 주소 (Full URL)                          | 인증          |
|------------------|--------|----------------------------------------------------|---------------|
| **특정 글의 댓글 목록**| `GET`  | `http://localhost/api/comments?postId={postId}`| 불필요        |
| **댓글 작성**        | `POST` | `http://localhost/api/comments`                  | ACTIVE_MEMBER |
| **댓글 수정**        | `PUT`  | `http://localhost/api/comments/{id}`             | 작성자/ADMIN  |
| **댓글 삭제**        | `DELETE`| `http://localhost/api/comments/{id}`            | 작성자/ADMIN  |

### 🎮 실시간 (WebSocket Service)

| 기능       | Protocol | 전체 요청 주소 (Full URL) | 인증 |
|------------|----------|---------------------------|------|
| **웹소켓 연결**| `WS`     | `ws://localhost/api/ws`   | 필요 |

### ❤️ 헬스체크 (모든 서비스)

| 기능           | Method | 전체 요청 주소 (Full URL)      | 인증   |
|----------------|--------|--------------------------------|--------|
| **서비스 상태 확인**| `GET`  | `http://localhost/api/health`  | 불필요 |
