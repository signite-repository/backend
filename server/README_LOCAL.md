# 🚀 Signite Backend 로컬 실행 가이드

## 📋 실행 환경

### 로컬 프로파일 설정 (H2 인메모리 DB)
```yaml
spring:
  profiles:
    active: local
  r2dbc:
    url: r2dbc:h2:mem:///testdb
    username: sa
    password: ""
  redis:
    host: localhost
    port: 6379
    password: ""
```

## 🏃‍♂️ 실행 방법

### 1. IDE에서 직접 실행
```kotlin
// SigniteBackendApplication.kt 파일을 우클릭 후 Run
// 또는 main 함수에서 실행
```

### 2. Gradle로 실행
```bash
# Windows 환경
./gradlew bootRun --args='--spring.profiles.active=local'

# 또는 환경변수 설정 후
set SPRING_PROFILES_ACTIVE=local
./gradlew bootRun
```

### 3. JAR 빌드 후 실행
```bash
./gradlew build -x test
java -jar build/libs/signite-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

## 🔧 외부 의존성 (선택사항)

로컬에서 Redis와 NATS를 사용하려면:

```bash
# Redis (Docker)
docker run -d --name redis -p 6379:6379 redis:alpine

# NATS (Docker)
docker run -d --name nats -p 4222:4222 -p 8222:8222 nats:alpine --jetstream
```

## 🧪 API 테스트

### Health Check
```bash
curl http://localhost:8080/api/test/health
```

### 응답 예시
```json
{
  "status": "OK",
  "service": "signite-backend",
  "timestamp": 1704067200000
}
```

## 📊 로컬 데이터베이스

H2 인메모리 DB 사용:
- 자동으로 `signite` 데이터베이스 생성
- `schema.sql` 초기화 스크립트 실행
- 애플리케이션 재시작시 데이터 초기화

## 🔍 로그 레벨

개발 환경에서 자세한 로그를 보려면:
```yaml
logging:
  level:
    com.signite.backend: DEBUG
    org.springframework.r2dbc: DEBUG
```

## 🚨 문제 해결

### 빌드 에러
```bash
# 빌드 캐시 정리
./gradlew clean

# IDE 캐시 정리 (IntelliJ)
File -> Invalidate Caches and Restart
```

### 포트 충돌
```bash
# 포트 사용 확인
netstat -ano | findstr :8080

# 프로세스 종료
taskkill /PID <PID번호> /F
``` 