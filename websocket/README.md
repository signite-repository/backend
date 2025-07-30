# Rust WebSocket 서버 (MSA)

실시간 3D 온라인 게임을 위한 고성능 WebSocket 서버입니다. Rust로 구현되었으며, Redis와 MongoDB를 사용하여 확장 가능한 MSA 아키텍처를 제공합니다.

## 🏗️ 아키텍처

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   프론트엔드     │────│   Istio Gateway │────│  WebSocket 서버  │
│  (React/Vue)    │    │   (Load Balancer)│    │    (Rust)       │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                                         │
                              ┌─────────────────────────┼─────────────────────────┐
                              │                         │                         │
                    ┌─────────▼──────────┐    ┌────────▼────────┐    ┌──────────▼───────────┐
                    │      Redis         │    │    MongoDB      │    │   Kubernetes Pod     │
                    │   (실시간 캐시)     │    │  (영속 저장소)   │    │    (Auto Scaling)    │
                    └────────────────────┘    └─────────────────┘    └──────────────────────┘
```

## ✨ 주요 기능

### 🎮 실시간 게임 기능
- **플레이어 상태 동기화**: 위치, 회전, 애니메이션 실시간 업데이트
- **룸 기반 세션**: 독립적인 게임 룸 관리
- **실시간 채팅**: 룸 내 플레이어 간 채팅

### 🚀 성능 및 확장성
- **Redis 캐싱**: 플레이어 상태 고속 캐싱
- **MongoDB 영속화**: 채팅 기록, 룸 정보 저장
- **수평 확장**: 쿠버네티스 기반 오토스케일링
- **로드 밸런싱**: Istio를 통한 지능형 트래픽 분산

### 🔒 보안 및 안정성
- **JWT 인증**: Istio 기반 토큰 검증
- **mTLS**: 서비스 간 암호화 통신
- **헬스체크**: 자동 장애 감지 및 복구
- **Graceful Shutdown**: 안전한 서버 종료

## 🛠️ 기술 스택

- **언어**: Rust 1.75+
- **WebSocket**: tokio-tungstenite
- **HTTP 서버**: Axum
- **데이터베이스**: MongoDB + Redis
- **컨테이너**: Docker
- **오케스트레이션**: Kubernetes
- **서비스 메시**: Istio
- **모니터링**: 내장 헬스체크

## 📋 요구사항

- Docker 20.10+
- Kubernetes 1.25+
- Istio 1.18+
- kubectl
- Rust 1.75+ (개발 시)

## 🚀 빠른 시작

### 1. 저장소 클론
```bash
git clone <repository-url>
cd socket_server
```

### 2. 배포 스크립트 실행
```bash
chmod +x deploy.sh
./deploy.sh
```

### 3. 서비스 확인
```bash
kubectl get all -n websocket
```

## 🔧 개발 환경 설정

### 로컬 개발
```bash
# 의존성 설치
cargo build

# 환경 변수 설정
export REDIS_URL=redis://localhost:6379
export MONGODB_URL=mongodb://localhost:27017
export MONGODB_DB_NAME=signight_websocket

# 서버 실행
cargo run
```

### Docker 개발
```bash
# Docker Compose로 실행 (Redis, MongoDB 포함)
docker-compose up -d

# 로그 확인
docker-compose logs -f websocket-server
```

## 📡 API 엔드포인트

### WebSocket
- **연결**: `ws://localhost:8080` 또는 `wss://ws.signight.com`

### HTTP API
- **헬스체크**: `GET /health`
- **활성 룸 조회**: `GET /rooms`
- **채팅 기록**: `POST /chat/history`

## 💬 WebSocket 메시지 형식

### 클라이언트 → 서버

**룸 입장**
```json
{
  "type": "Join",
  "room_id": "room123",
  "name": "플레이어1",
  "color": "#FF0000"
}
```

**플레이어 상태 업데이트**
```json
{
  "type": "Update",
  "state": {
    "name": "플레이어1",
    "color": "#FF0000",
    "position": [0.0, 0.0, 0.0],
    "rotation": [1.0, 0.0, 0.0, 0.0],
    "animation": "walking",
    "velocity": [1.0, 0.0, 0.0]
  }
}
```

**채팅 메시지**
```json
{
  "type": "Chat",
  "message": "안녕하세요!"
}
```

### 서버 → 클라이언트

**환영 메시지**
```json
{
  "type": "Welcome",
  "client_id": "uuid-123",
  "room_state": [...],
  "chat_history": [...]
}
```

## 🔧 환경 변수

| 변수명 | 기본값 | 설명 |
|--------|--------|------|
| `SERVER_HOST` | `0.0.0.0` | 서버 바인드 주소 |
| `SERVER_PORT` | `8080` | WebSocket 포트 |
| `HTTP_PORT` | `3001` | HTTP API 포트 |
| `REDIS_URL` | `redis://redis:6379` | Redis 연결 URL |
| `MONGODB_URL` | `mongodb://mongo:27017` | MongoDB 연결 URL |
| `MONGODB_DB_NAME` | `signight_websocket` | MongoDB 데이터베이스명 |
| `JWT_SECRET` | - | JWT 검증 시크릿 키 |
| `JWT_ISSUER` | `https://auth.signite.com` | JWT 발급자 |
| `RUST_LOG` | `info` | 로그 레벨 |

## 📊 모니터링

### 헬스체크
```bash
curl http://localhost:3001/health
```

### 메트릭
- 연결된 클라이언트 수
- 활성 룸 수
- 메시지 처리량
- 데이터베이스 연결 상태

### 로그
```bash
# 쿠버네티스 로그
kubectl logs -f deployment/websocket-server -n websocket

# 로컬 로그
RUST_LOG=debug cargo run
```

## 🐛 트러블슈팅

### 일반적인 문제

**연결 실패**
```bash
# 포트 확인
kubectl get svc -n websocket
netstat -tulpn | grep 8080
```

**데이터베이스 연결 오류**
```bash
# MongoDB 상태 확인
kubectl exec -it mongo-0 -n websocket -- mongosh

# Redis 상태 확인
kubectl exec -it deployment/redis -n websocket -- redis-cli ping
```

**성능 이슈**
- Redis 메모리 사용량 확인
- MongoDB 인덱스 최적화
- 쿠버네티스 리소스 한계 조정

## 🔄 배포 및 업데이트

### 배포 전략
1. **롤링 업데이트**: 무중단 배포
2. **Blue-Green**: 안전한 전환
3. **Canary**: 점진적 출시

### CI/CD 파이프라인
```bash
# 빌드
docker build -t websocket-server:$VERSION .

# 테스트
cargo test

# 배포
kubectl set image deployment/websocket-server websocket-server=websocket-server:$VERSION -n websocket
```

## 📈 성능 튜닝

### Rust 최적화
- `--release` 빌드 사용
- 메모리 풀링 적용
- 비동기 I/O 최적화

### 쿠버네티스 최적화
- HPA (Horizontal Pod Autoscaler) 설정
- 리소스 요청/한계 튜닝
- 노드 어피니티 설정

### 데이터베이스 최적화
- Redis 연결 풀 크기 조정
- MongoDB 인덱스 생성
- 쿼리 최적화

## 🤝 기여 가이드

1. 포크 후 브랜치 생성
2. 코드 변경 및 테스트
3. 커밋 메시지 규칙 준수
4. Pull Request 생성

## 📄 라이선스

MIT License

## 🆘 지원

- GitHub Issues: 버그 리포트 및 기능 요청
- Discord: 실시간 지원 채널
- Documentation: 상세 API 문서 