# 🎮 WebSocket Service API 매뉴얼

## 1. 개요

- **담당 서비스**: `websocket-server`
- **기술 스택**: Rust, Tokio, Axum, MongoDB, Redis
- **주요 기능**:
  - 실시간 3D 게임을 위한 플레이어 상태 동기화
  - 룸 기반의 실시간 채팅 기능
  - 고성능, 저지연 메시지 브로드캐스팅

---

## 2. WebSocket API

- **연결 URL**: `ws://localhost/api/ws` (Istio Gateway 경유)
- **메시지 형식**: JSON (Text Frame)

### **클라이언트 → 서버 메시지**

#### **1. 룸 입장 (Join)**
- **설명**: 특정 룸에 입장하고, 플레이어 정보를 서버에 등록합니다.
- **메시지**:
  ```json
  {
    "type": "Join",
    "room_id": "game-room-123",
    "name": "플레이어1",
    "color": "#FF5733"
  }
  ```

#### **2. 상태 업데이트 (Update)**
- **설명**: 플레이어의 위치, 회전 등 상태 정보를 서버로 전송하여 다른 클라이언트에게 브로드캐스팅합니다.
- **메시지**:
  ```json
  {
    "type": "Update",
    "state": {
      "name": "플레이어1",
      "position": [10.5, 0.0, -5.2],
      "rotation": [1.0, 0.0, 0.0, 0.0],
      "animation": "walking"
    }
  }
  ```

#### **3. 채팅 (Chat)**
- **설명**: 채팅 메시지를 전송합니다.
- **메시지**:
  ```json
  {
    "type": "Chat",
    "message": "안녕하세요! 반갑습니다."
  }
  ```

### **서버 → 클라이언트 메시지**

#### **1. 환영 메시지 (Welcome)**
- **설명**: 룸 입장 성공 시, 현재 룸의 상태(다른 플레이어 목록)와 채팅 기록을 함께 전송합니다.
- **메시지**:
  ```json
  {
    "type": "Welcome",
    "client_id": "uuid-for-new-client",
    "room_state": [ { "client_id": "...", "state": { ... } } ],
    "chat_history": [ { "username": "...", "message": "..." } ]
  }
  ```

#### **2. 다른 플레이어 상태 업데이트 (PlayerUpdate)**
- **설명**: 다른 플레이어의 상태가 변경될 때마다 수신합니다.
- **메시지**:
  ```json
  {
    "type": "PlayerUpdate",
    "client_id": "other-player-uuid",
    "state": { "position": [11.0, 0.0, -5.0], ... }
  }
  ```

---

## 3. 보조 HTTP API

- **Base URL**: `/api/ws-http` (임의의 경로, 게이트웨이 설정 필요)

| Method | Endpoint        | 설명             |
|--------|-----------------|------------------|
| `GET`  | `/health`       | 서버 상태 확인   |
| `GET`  | `/rooms`        | 활성화된 룸 목록 |
| `POST` | `/chat/history` | 특정 룸 채팅 기록|

---

## 4. 데이터베이스

### **MongoDB**
- **`chat_messages` 컬렉션**: 모든 채팅 메시지를 영구 저장합니다.
- **`rooms` 컬렉션**: 현재 활성화된 룸의 정보를 저장합니다.

### **Redis**
- **플레이어 상태 캐시**: 각 플레이어의 최신 상태를 Key-Value 형태로 캐시하여 빠른 조회를 지원합니다. (`room:{room_id}:player:{client_id}`)
- **Pub/Sub (확장용)**: WebSocket 서버를 여러 인스턴스로 확장할 경우, Redis Pub/Sub을 통해 서버 간 메시지를 브로드캐스팅합니다.
