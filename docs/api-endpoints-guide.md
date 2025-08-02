# Signight API 통합 엔드포인트 가이드

## 개요
Istio Gateway를 통한 통합 API 엔드포인트를 제공합니다.
Docker Desktop 환경에서는 별도의 포트포워딩 없이 `localhost`로 접근 가능합니다.

## 기본 URL
- 개발: `http://localhost`
- 운영: `https://api.signight.com`

## API 엔드포인트

### 1. 인증 서비스 (Auth Service)
```
POST   /api/auth/login         # 로그인
POST   /api/auth/logout        # 로그아웃
POST   /api/auth/refresh       # 토큰 갱신
POST   /api/auth/register      # 회원가입
GET    /api/auth/me            # 현재 사용자 정보
```

### 2. 카테고리 서비스 (Category Service)
```
GET    /api/categories         # 카테고리 목록
POST   /api/categories         # 카테고리 생성
GET    /api/categories/{id}    # 카테고리 상세
PUT    /api/categories/{id}    # 카테고리 수정
DELETE /api/categories/{id}    # 카테고리 삭제
```

### 3. 게시글 서비스 (Post Service)
```
GET    /api/posts              # 게시글 목록
POST   /api/posts              # 게시글 작성
GET    /api/posts/{id}         # 게시글 상세
PUT    /api/posts/{id}         # 게시글 수정
DELETE /api/posts/{id}         # 게시글 삭제
GET    /api/posts/search       # 게시글 검색
```

### 4. 댓글 서비스 (Comment Service)
```
GET    /api/comments           # 댓글 목록
POST   /api/comments           # 댓글 작성
PUT    /api/comments/{id}      # 댓글 수정
DELETE /api/comments/{id}      # 댓글 삭제
```

### 5. 웹소켓 서비스 (WebSocket)
```
WS     /api/ws                 # 웹소켓 연결
```

### 6. 헬스체크 (모든 서비스 공통)
```
GET    /api/health             # 서비스 상태 확인
```

## 프론트엔드 사용 예시

### axios 설정
```javascript
import axios from 'axios';

const api = axios.create({
  baseURL: process.env.NODE_ENV === 'production' 
    ? 'https://api.signight.com' 
    : 'http://localhost',
  headers: {
    'Content-Type': 'application/json',
  },
});

// 토큰 자동 첨부
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default api;
```

### API 호출 예시
```javascript
// 로그인
const login = async (email, password) => {
  const response = await api.post('/api/auth/login', { email, password });
  return response.data;
};

// 카테고리 목록 조회
const getCategories = async () => {
  const response = await api.get('/api/categories');
  return response.data;
};

// 게시글 작성
const createPost = async (postData) => {
  const response = await api.post('/api/posts', postData);
  return response.data;
};
```

## 개발 환경 설정

### 1. 서비스 배포
```bash
# 모든 서비스 배포
kubectl apply -f k8s/

# 상태 확인
kubectl get pods
kubectl get svc -n istio-system
```

### 2. 접속 확인
```bash
# 헬스체크
curl http://localhost/api/health

# API 테스트
curl http://localhost/api/categories
```

## 문제 해결

### Istio Gateway가 작동하지 않을 때
```bash
# Istio 상태 확인
kubectl get pods -n istio-system

# Gateway 재시작
kubectl rollout restart deployment/istio-ingressgateway -n istio-system
```

### 서비스 연결이 안 될 때
```bash
# VirtualService 확인
kubectl get virtualservice

# 서비스 엔드포인트 확인
kubectl get endpoints
```