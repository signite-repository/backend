# 🔐 Auth Service API 매뉴얼

## 1. 개요

- **담당 서비스**: `auth-service`
- **기술 스택**: Kotlin, Spring WebFlux, MariaDB
- **주요 기능**:
  - 사용자 회원가입 및 로그인 처리
  - JWT (Access Token, Refresh Token) 발급 및 검증
  - Istio와 연동하여 인증 헤더 기반의 사용자 정보 제공

---

## 2. API 엔드포인트

- **Base URL**: `/api/auth`

### **회원가입**
- `POST /register`
- **설명**: 새로운 사용자를 시스템에 등록합니다.
- **인증**: 불필요
- **Request Body**:
  ```json
  {
    "username": "newuser",
    "email": "new.user@example.com",
    "password": "password123"
  }
  ```
- **Response (201 CREATED)**:
  ```json
  {
    "id": "124",
    "username": "newuser",
    "email": "new.user@example.com",
    "roles": ["USER"],
    "enabled": true
  }
  ```

### **로그인**
- `POST /login`
- **설명**: 사용자 인증 후 JWT 토큰을 발급합니다.
- **인증**: 불필요
- **Request Body**:
  ```json
  {
    "username": "testuser",
    "password": "password123"
  }
  ```
- **Response (200 OK)**:
  - **Headers**:
    - `Authorization`: `Bearer {ACCESS_TOKEN}`
    - `X-Refresh-Token`: `{REFRESH_TOKEN}`
  - **Body**:
    ```json
    {
      "id": "123",
      "username": "testuser",
      "roles": ["USER", "SIG_ADMIN"]
    }
    ```

### **내 정보 조회**
- `GET /me`
- **설명**: 현재 인증된 사용자의 정보를 조회합니다.
- **인증**: 필요 (Access Token)
- **Response (200 OK)**:
  ```json
  {
    "id": "123",
    "username": "testuser",
    "email": "test@example.com",
    "roles": ["USER", "SIG_ADMIN"],
    "enabled": true
  }
  ```

---

## 3. 데이터베이스 (MariaDB)

### **`users` 테이블**
사용자 기본 정보를 저장합니다.

| Column          | Type                | 설명                           |
|-----------------|---------------------|--------------------------------|
| `id`            | `BIGINT` (PK)       | 사용자 고유 ID                 |
| `username`      | `VARCHAR(50)`       | 사용자 이름 (고유)             |
| `email`         | `VARCHAR(255)`      | 이메일 (고유)                  |
| `password`      | `VARCHAR(255)`      | 해시된 비밀번호                |
| `roles`         | `JSON`              | 사용자 권한 목록               |
| `enabled`       | `BOOLEAN`           | 계정 활성화 여부               |
| `created_at`    | `TIMESTAMP`         | 생성 일시                      |
| `organization_id`| `BIGINT`           | 소속 조직 ID (멀티테넌트용)    |

### **`user_roles` 테이블**
7단계 권한 시스템에 따른 역할을 저장합니다.

| Column      | Type                                       | 설명         |
|-------------|--------------------------------------------|--------------|
| `id`        | `BIGINT` (PK)                              | 역할 ID      |
| `user_id`   | `BIGINT` (FK)                              | 사용자 ID    |
| `role_name` | `ENUM(...)`                                | 역할 이름    |

---

## 4. 핵심 로직

- **JWT 발급**: `JwtService`가 Access/Refresh 토큰 생성을 담당합니다.
- **비밀번호 암호화**: Spring Security의 `BCryptPasswordEncoder`를 사용합니다.
- **Istio 연동**: 로그인 성공 시, Istio가 검증할 수 있는 JWT를 생성하며, 이후의 모든 요청은 Istio Gateway에서 토큰이 1차 검증됩니다.
