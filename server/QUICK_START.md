# ⚡ Signite Backend 빠른 시작

## 🎯 즉시 실행 (IDE 사용)

### 1. 프로젝트 열기
```
IntelliJ IDEA 또는 VS Code에서 backend 폴더를 프로젝트로 열기
```

### 2. 실행 설정
**IntelliJ IDEA:**
- `SigniteBackendApplication.kt` 파일 열기
- `main` 함수 옆의 ▶️ 버튼 클릭
- **Edit Configurations**에서 **Program arguments** 추가:
  ```
  --spring.profiles.active=local
  ```

**VS Code:**
- F5 누르거나 Run and Debug 탭에서 실행
- `launch.json`에 설정 추가

### 3. 실행 확인
```bash
# 브라우저에서 접속
http://localhost:8080/api/test/health

# 또는 curl로 확인
curl http://localhost:8080/api/test/health
```

## 🔧 설정 확인

### 현재 로컬 설정 (H2 인메모리)
```yaml
spring:
  profiles:
    active: local
  r2dbc:
    url: r2dbc:h2:mem:///testdb  # 인메모리 DB
    username: sa
    password: ""
  redis:
    host: localhost  # 연결 실패해도 앱 실행됨
    port: 6379
```

### 외부 의존성 (선택사항)
```bash
# Redis (캐시 기능 사용하려면)
docker run -d --name redis -p 6379:6379 redis:alpine

# NATS (이벤트 기능 사용하려면)  
docker run -d --name nats -p 4222:4222 -p 8222:8222 nats:alpine --jetstream
```

## 🧪 API 테스트

### 기본 헬스체크
```http
GET http://localhost:8080/api/test/health
```

### 인증 테스트  
```http
GET http://localhost:8080/api/test/auth
```

### 권한 테스트
```http
GET http://localhost:8080/api/test/role?role=ACTIVE_MEMBER
```

## 🚨 문제 해결

### 빌드 에러 발생시
1. **IDE 캐시 정리:**
   - IntelliJ: `File → Invalidate Caches and Restart`
   - VS Code: `Ctrl+Shift+P → Java: Reload Projects`

2. **Gradle 캐시 정리:**
   ```bash
   ./gradlew clean
   ```

3. **Windows 파일 잠금 해제:**
   - IDE 완전 종료 후 재시작
   - 안티바이러스 실시간 검사 일시 해제

### 포트 8080 사용 중일 때
```bash
# 포트 사용 프로세스 확인
netstat -ano | findstr :8080

# 프로세스 종료 (PID 확인 후)
taskkill /PID <PID번호> /F
```

## 🎯 다음 단계

1. ✅ **로컬 실행 성공**
2. 🧪 **API 테스트**  
3. 📊 **DB 스키마 확인**
4. 🔧 **테스트 코드 작성**
5. 🚀 **K8s 배포** 