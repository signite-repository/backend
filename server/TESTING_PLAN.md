# 🧪 Signite Backend 테스트 전략

## 🎯 현재 상황

### ✅ 완료된 작업
- K8s 인프라 구성 (MariaDB, Redis, NATS, Secrets)
- 로컬 실행 가이드 작성
- API 테스트 파일 준비
- 기본 테스트 템플릿 준비

### 🚧 진행 중
- 빌드 환경 문제 해결
- 테스트 코드 작성

## 📊 테스트 커버리지 목표

### 1. 단위 테스트 (Unit Tests)
```
✅ PostServiceTest.kt (기존)
✅ ValidationServiceTest.kt (기존)  
✅ PostRepositoryTest.kt (기존)
✅ PostHandlerTest.kt (기존)

🚧 추가 작성 필요:
- AuthServiceTest.kt
- CategoryServiceTest.kt  
- CommentServiceTest.kt
- TagServiceTest.kt
- JwtServiceTest.kt
- UserContextServiceTest.kt
- EventServiceTest.kt
- UserRoleServiceTest.kt
```

### 2. 리포지토리 테스트
```
✅ PostRepositoryTest.kt (기존)

🚧 추가 작성 필요:
- UserRepositoryTest.kt
- CategoryRepositoryTest.kt
- CommentRepositoryTest.kt
- TagRepositoryTest.kt
- UserRoleRepositoryTest.kt
- CacheRepositoryTest.kt
```

### 3. 핸들러 테스트 (WebFlux)
```
✅ PostHandlerTest.kt (기존)

🚧 추가 작성 필요:
- AuthHandlerTest.kt
- CategoryHandlerTest.kt
- CommentHandlerTest.kt
- TagHandlerTest.kt
- TestHandlerTest.kt
- UploadHandlerTest.kt
- JwksHandlerTest.kt
```

### 4. 통합 테스트
```
✅ IntegrationTest.kt (기존)

🚧 확장 필요:
- MSA 이벤트 통합 테스트
- 캐시 통합 테스트
- 권한 시스템 통합 테스트
- 전체 워크플로우 테스트
```

## 🔧 테스트 작성 가이드

### 명명 규칙
```kotlin
@Test
fun `한글로_명확한_테스트_시나리오_설명`() {
    // Given - 테스트 데이터 준비
    // When - 실제 동작 실행  
    // Then - 결과 검증
}
```

### Mock 사용 패턴
```kotlin
@ExtendWith(MockitoExtension::class)
class ServiceTest {
    @Mock
    private lateinit var repository: Repository
    
    private lateinit var service: Service
    
    @BeforeEach
    fun setUp() {
        service = Service(repository)
        reset(repository)
    }
}
```

### Reactive 테스트
```kotlin
StepVerifier.create(service.method())
    .expectNextMatches { result ->
        // 검증 로직
        result.field == expectedValue
    }
    .verifyComplete()
```

## 📈 테스트 실행 전략

### 1. 로컬 개발
```bash
# 전체 테스트 실행
./gradlew test

# 특정 테스트 클래스
./gradlew test --tests "AuthServiceTest"

# 특정 테스트 메서드
./gradlew test --tests "AuthServiceTest.사용자_생성이_성공적으로_처리된다"
```

### 2. 지속적 통합 (CI)
```yaml
# GitHub Actions에서 실행
- name: Run Tests
  run: ./gradlew test jacocoTestReport
  
- name: Upload Coverage
  uses: codecov/codecov-action@v2
```

### 3. 성능 테스트
```kotlin
// 반응형 스트림 성능 테스트
@Test
fun `대량_데이터_처리_성능_테스트`() {
    val startTime = System.currentTimeMillis()
    
    StepVerifier.create(
        service.processLargeData(10000)
    ).expectNextCount(10000)
     .verifyComplete()
     
    val duration = System.currentTimeMillis() - startTime
    assertThat(duration).isLessThan(5000) // 5초 이내
}
```

## 🎯 테스트 데이터 전략

### 테스트 프로파일 설정
```yaml
# application-test.yml
spring:
  r2dbc:
    url: r2dbc:h2:mem:///testdb
  redis:
    host: localhost
    port: 6370  # 다른 포트 사용
```

### Mock 데이터 팩토리
```kotlin
object TestDataFactory {
    fun createTestUser(id: Int = 1) = User(
        id = id,
        username = "testuser$id",
        email = "test$id@example.com",
        hashedPassword = "hashed123",
        imageUrl = "profile$id.jpg"
    )
    
    fun createTestPost(userId: Int = 1) = Post(
        title = "테스트 게시글",
        content = "테스트 내용",
        userId = userId
    )
}
```

## 🚀 실행 순서

### 1단계: 환경 설정
```bash
# IDE에서 프로젝트 열기
# Java 17, Kotlin 설정 확인
# Gradle 동기화
```

### 2단계: 로컬 실행
```bash
# 애플리케이션 실행 (IDE 또는 Gradle)
./gradlew bootRun --args='--spring.profiles.active=local'
```

### 3단계: API 테스트
```bash
# api-test.http 파일로 테스트
# 헬스체크 → 회원가입 → 로그인 → 기능 테스트 순서
```

### 4단계: 단위 테스트 작성
```bash
# 빌드 성공 확인 후 테스트 코드 작성
# 서비스 → 리포지토리 → 핸들러 순서
```

### 5단계: K8s 배포 테스트
```bash
# 로컬 환경에서 테스트 성공 후
./dev.sh setup  # K8s 환경 구성
./dev.sh deploy # 애플리케이션 배포
```

## 🔍 주요 테스트 시나리오

### 인증/인가 플로우
1. 회원가입 → JWT 토큰 발급
2. 로그인 → JWT 토큰 검증  
3. 권한별 API 접근 제어
4. 토큰 만료 처리

### 게시판 기능
1. 게시글 CRUD (권한 검증 포함)
2. 댓글 시스템 
3. 카테고리/태그 관리
4. 파일 업로드

### MSA 이벤트
1. 게시글 생성 이벤트 발행
2. 댓글 생성 이벤트 발행  
3. 사용자 가입 이벤트 발행
4. 이벤트 구독 및 처리

### 캐시 시스템
1. 카테고리 캐시 동작
2. 사용자 정보 캐시
3. 캐시 무효화 로직

이 계획에 따라 단계별로 테스트를 진행하면 완전한 테스트 커버리지를 달성할 수 있습니다! 🎯 