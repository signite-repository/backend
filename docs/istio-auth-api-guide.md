# Istio 기반 인증 시스템과 REST API 구현 가이드

## 개요

✅ **Istio 설치 완료**: 성공적으로 Istio가 설치되었습니다!
✅ **REST API 인증 가능**: `/api/auth/login`, `/api/auth/register`, `/api/auth/check` 엔드포인트 구현 가능

## 1. Istio와 애플리케이션 레벨 인증의 차이

### Istio가 제공하는 것:
- **서비스 간 인증 (mTLS)**: 마이크로서비스 간 통신 암호화
- **JWT 토큰 검증**: 이미 발급된 JWT 토큰의 유효성 검증
- **권한 부여 (AuthorizationPolicy)**: 토큰의 claims 기반 접근 제어
- **트래픽 관리**: 라우팅, 로드밸런싱, 서킷브레이커

### Istio가 제공하지 않는 것:
- **사용자 로그인/회원가입 로직**
- **JWT 토큰 발급**
- **사용자 정보 저장/관리**
- **비밀번호 검증**

## 2. REST API 인증 구현 방법

### 옵션 1: Category Service에 인증 API 추가 (구현됨)
```kotlin
// services/category-service/src/main/kotlin/com/signite/categoryservice/web/rest/AuthResource.kt

@RestController
@RequestMapping("/api/auth")
class AuthResource {
    
    @PostMapping("/register")  // 회원가입
    @PostMapping("/login")     // 로그인
    @GetMapping("/check")       // 인증 확인
    @PostMapping("/refresh")    // 토큰 갱신
    @PostMapping("/logout")     // 로그아웃
}
```

### 옵션 2: 별도 Auth Service 구축
```yaml
# k8s/auth-service/auth-service.yaml
# 전용 인증 마이크로서비스
```

### 옵션 3: 외부 인증 서비스 사용
- **Keycloak**: 오픈소스 IdP
- **Auth0**: SaaS 인증 서비스
- **Firebase Auth**: Google 인증 서비스

## 3. 현재 구현 상태

### 필요한 추가 구성요소:

#### JWT 서비스 구현
```kotlin
// JwtService.kt
@Service
class JwtService {
    @Value("\${jwt.secret}")
    private lateinit var secret: String
    
    fun generateToken(user: User): String {
        // JWT 토큰 생성 로직
    }
    
    fun validateToken(token: String): Boolean {
        // 토큰 검증 로직
    }
}
```

#### User 서비스 및 Repository
```kotlin
// UserService.kt
@Service
class UserService(private val userRepository: UserRepository) {
    fun createUser(user: User): Mono<User>
    fun findByUsername(username: String): Mono<User>
    fun existsByUsername(username: String): Mono<Boolean>
}
```

#### Security 설정
```kotlin
// SecurityConfig.kt
@Configuration
@EnableWebFluxSecurity
class SecurityConfig {
    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .cors().and()
            .csrf().disable()
            .authorizeExchange()
            .pathMatchers("/api/auth/**").permitAll()
            .pathMatchers("/actuator/health").permitAll()
            .anyExchange().authenticated()
            .and()
            .build()
    }
}
```

## 4. Istio와 통합하기

### Istio AuthorizationPolicy 업데이트
```yaml
# k8s/category-service/istio.yaml 수정
spec:
  rules:
  # 인증 API는 누구나 접근 가능
  - to:
    - operation:
        methods: ["POST"]
        paths: ["/api/auth/login", "/api/auth/register"]
  
  # JWT 토큰이 있는 요청만 허용
  - to:
    - operation:
        methods: ["GET", "POST", "PUT", "DELETE"]
        paths: ["/api/v1/categories/*"]
    when:
    - key: request.headers[authorization]
      notValues: [""]
```

### RequestAuthentication 설정
```yaml
apiVersion: security.istio.io/v1beta1
kind: RequestAuthentication
metadata:
  name: category-jwt-auth
spec:
  selector:
    matchLabels:
      app: category-service
  jwtRules:
  - issuer: "https://auth.signight.com"
    jwksUri: "http://category-service-service.default.svc.cluster.local/api/auth/jwks"
    audiences:
    - "category-service"
```

## 5. 구현 단계

### Step 1: 필요한 의존성 추가
```gradle
// build.gradle.kts
dependencies {
    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")
    
    // Security
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.security:spring-security-crypto")
}
```

### Step 2: User 도메인 모델 생성
```kotlin
@Document("users")
data class User(
    @Id
    val id: String? = null,
    val username: String,
    val email: String,
    val password: String,
    val roles: List<String> = listOf("USER"),
    val enabled: Boolean = true,
    val createdAt: Instant = Instant.now()
)
```

### Step 3: 테스트
```bash
# 회원가입
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@signight.com",
    "password": "password123"
  }'

# 로그인
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'

# 응답 예시
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": "123",
    "username": "testuser",
    "email": "test@signight.com",
    "roles": ["USER"]
  }
}

# 인증 확인
curl -X GET http://localhost:8081/api/auth/check \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..."
```

## 6. Istio 통합 아키텍처

```
사용자 → Istio Gateway → Virtual Service → 
    ↓
    ├─ /api/auth/* → Category Service (인증 불필요)
    │   ├─ /login → JWT 발급
    │   ├─ /register → 회원가입
    │   └─ /check → 토큰 검증
    │
    └─ /api/v1/* → Category Service (JWT 필요)
        └─ Istio가 JWT 검증 → 서비스로 전달
```

## 7. 보안 고려사항

1. **JWT Secret**: 환경변수로 관리, 정기적 로테이션
2. **비밀번호**: BCrypt/SCrypt로 해싱
3. **HTTPS**: Istio Gateway에서 TLS 종료
4. **토큰 만료**: Access Token 1시간, Refresh Token 7일
5. **Rate Limiting**: Istio EnvoyFilter로 구현

## 결론

✅ **가능합니다!** REST API로 `/api/auth/login`, `/api/auth/register`, `/api/auth/check` 모두 구현 가능합니다.

- **Istio**는 JWT 검증과 권한 부여를 담당
- **애플리케이션**은 JWT 발급과 사용자 관리를 담당
- 두 레이어가 협력하여 완전한 인증 시스템 구성

이미 Category Service에 기본 구조를 추가했으니, JWT 서비스와 User Repository만 구현하면 바로 사용 가능합니다!