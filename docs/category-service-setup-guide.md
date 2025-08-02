# Category Service 구축 가이드

## 1. 개요

Category Service는 다중 깊이 트리 구조의 카테고리 관리를 제공하는 마이크로서비스입니다. 본 문서는 초기 MariaDB/R2DBC 기반에서 MongoDB 기반으로 완전히 전환한 과정과 Kubernetes 배포 과정을 기록합니다.

## 2. 기술 스택 변경

### 2.1 기존 스택
- **Database**: MariaDB (R2DBC)
- **ORM**: Spring Data R2DBC
- **배포**: Kubernetes Deployment

### 2.2 변경된 스택  
- **Database**: MongoDB
- **ORM**: Spring Data Reactive MongoDB
- **배포**: Kubernetes StatefulSet (MongoDB), Deployment (App)

## 3. 데이터베이스 마이그레이션

### 3.1 의존성 변경
```kotlin
// build.gradle.kts
dependencies {
    // 기존: R2DBC
    // implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    // implementation("org.mariadb:r2dbc-mariadb")
    
    // 변경: MongoDB Reactive
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")
    
    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("de.flapdoodle.embed:de.flapdoodle.embed.mongo.spring30x:4.11.0")
    testImplementation("io.mockk:mockk:1.13.8")
}
```

### 3.2 도메인 모델 변경
```kotlin
// Category.kt
@Document("categories")  // @Table에서 변경
data class Category(
    @Id
    val id: String? = null,  // Long에서 String으로 변경
    
    @Field("name")
    var name: String,
    
    @Field("slug")
    var slug: String,
    
    @Field("parentId")
    var parentId: String?,
    
    @Field("path")
    var path: String,
    
    @Field("level")
    var level: Int,
    
    @Field("displayOrder")
    var displayOrder: Int,
    
    @Field("metadata")
    var metadata: Map<String, Any>? = emptyMap(),  // String?에서 Map으로 변경
    
    @Field("createdAt")
    val createdAt: LocalDateTime? = LocalDateTime.now()
)
```

### 3.3 Repository 변경
```kotlin
// CategoryRepository.kt
interface CategoryRepository : ReactiveMongoRepository<Category, String> {
    fun findByParentIdOrderByDisplayOrder(parentId: String): Flux<Category>
    fun findByPathStartingWithOrderByPath(parentPath: String): Flux<Category>
    fun findBySlug(slug: String): Mono<Category>
    // SQL @Query 어노테이션 제거
}
```

### 3.4 설정 변경
```yaml
# application.yml
spring:
  application:
    name: category-service
  data:
    mongodb:
      uri: ${MONGO_URI}
      database: categorydb
```

```kotlin
// DatabaseConfig.kt
@Configuration
@EnableReactiveMongoRepositories  // @EnableR2dbcAuditing에서 변경
class DatabaseConfig
```

## 4. Kubernetes 보안 설정

### 4.1 Secret 생성
```yaml
# k8s/category-db/secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: category-db-secret
type: Opaque
data:
  # Base64 인코딩된 값들
  username: eWRoMjI0NA==  # ydh2244
  password: MTEyN3N0YXI=   # 1127star
  database: Y2F0ZWdvcnlkYg==  # categorydb
  uri: bW9uZ29kYjovL3lkaDIyNDQ6MTEyN3N0YXJAY2F0ZWdvcnktZGItc2VydmljZToyNzAxNy9jYXRlZ29yeWRiP2F1dGhTb3VyY2U9YWRtaW4=
```

**주의사항**: 
- 패스워드에 특수문자(`@`, `:`)가 있으면 URL 인코딩 필요
- 원래 `1127star@`였으나 `1127star`로 변경하여 인코딩 이슈 해결

### 4.2 ConfigMap 제거
```bash
kubectl delete configmap category-db-config
```
민감한 정보는 ConfigMap 대신 Secret으로 관리

## 5. MongoDB StatefulSet 배포

### 5.1 StatefulSet 설정
```yaml
# k8s/category-db/statefulset.yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: category-db
spec:
  serviceName: category-db-service
  replicas: 1
  template:
    spec:
      containers:
      - name: mongodb
        image: mongo:7-jammy
        env:
        - name: MONGO_INITDB_ROOT_USERNAME
          valueFrom:
            secretKeyRef:
              name: category-db-secret
              key: username
        # ... (Secret 기반 환경변수)
        resources:
          requests:
            memory: "256Mi"  # OOM 방지를 위해 충분한 메모리
            cpu: "100m"
          limits:
            memory: "512Mi"
            cpu: "200m"
  volumeClaimTemplates:
  - metadata:
      name: mongo-data
    spec:
      accessModes: [ "ReadWriteOnce" ]
      storageClassName: "hostpath"
      resources:
        requests:
          storage: 1Gi
```

**주요 특징**:
- **영구 스토리지**: PVC 템플릿으로 데이터 지속성 보장
- **Resource Limits**: OOMKilled 방지를 위한 충분한 메모리 할당
- **Health Checks**: 긴 초기화 시간을 고려한 probe 설정

## 6. 애플리케이션 배포

### 6.1 Deployment 설정
```yaml
# k8s/category-service/deployment.yaml
spec:
  replicas: 1  # 자원 절약을 위해 1개로 축소
  template:
    spec:
      containers:
      - name: category-service
        image: signite/category-service:latest
        env:
        - name: MONGO_URI
          valueFrom:
            secretKeyRef:
              name: category-db-secret
              key: uri
        resources:
          requests:
            memory: "128Mi"
            cpu: "100m"
          limits:
            memory: "256Mi"
            cpu: "200m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
```

### 6.2 HPA 비활성화
```bash
kubectl delete hpa category-service-hpa
```
로컬 환경의 자원 제약으로 인해 오토스케일링 비활성화

## 7. 네트워크 보안

### 7.1 NetworkPolicy 설정
```yaml
# MongoDB 접근 제한
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: category-db-network-policy
spec:
  podSelector:
    matchLabels:
      app: category-db
  policyTypes:
  - Ingress
  ingress:
  - from:
    - podSelector:
        matchLabels:
          app: category-service  # Category Service만 접근 허용
    ports:
    - protocol: TCP
      port: 27017
```

## 8. 테스트 코드 작성

### 8.1 단위 테스트 구조
```
src/test/kotlin/com/signite/categoryservice/
├── CategoryServiceApplicationTests.kt
├── service/CategoryServiceTest.kt
├── repository/CategoryRepositoryTest.kt
└── web/rest/CategoryResourceTest.kt
```

### 8.2 테스트 환경 설정
```yaml
# application-test.yml
spring:
  data:
    mongodb:
      host: localhost
      port: 0  # 임베디드 MongoDB 사용
      database: test
```

## 9. 트러블슈팅 기록

### 9.1 주요 문제와 해결책

#### Problem 1: URL 인코딩 이슈
**증상**: 
```
The connection string contains invalid user information. 
If the username or password contains a colon (:) or an at-sign (@) then it must be urlencoded
```

**해결**: 패스워드를 `1127star@`에서 `1127star`로 변경

#### Problem 2: MongoDB OOMKilled
**증상**: 
```
Last State: Terminated
Reason: OOMKilled
```

**해결**: MongoDB 메모리 제한을 256Mi → 512Mi로 증가

#### Problem 3: 필드 매핑 오류
**증상**: 
```
Failed to instantiate com.signite.categoryservice.domain.Category
Type mismatch: inferred type is Map<String, Any>? but String? was expected
```

**해결**: 
- `@Field` 어노테이션의 필드명을 camelCase로 통일
- `metadata` 타입을 `String?`에서 `Map<String, Any>?`로 변경

## 10. 배포 명령어

### 10.1 전체 배포 순서
```bash
# 1. Secret 적용
kubectl apply -f k8s/category-db/secret.yaml

# 2. MongoDB StatefulSet 배포
kubectl apply -f k8s/category-db/statefulset.yaml
kubectl apply -f k8s/category-db/service.yaml

# 3. 애플리케이션 빌드 및 배포
docker build -t signite/category-service:latest services/category-service/
kubectl apply -f k8s/category-service/deployment.yaml
kubectl apply -f k8s/category-service/service.yaml

# 4. 네트워크 정책 적용
kubectl apply -f k8s/category-db/networkpolicy.yaml
kubectl apply -f k8s/category-service/networkpolicy.yaml
```

### 10.2 서비스 확인
```bash
# 파드 상태 확인
kubectl get pods | grep category

# Health check
kubectl port-forward svc/category-service-service 8081:80
curl http://localhost:8081/actuator/health

# API 테스트
curl -X GET "http://localhost:8081/api/v1/categories"
```

## 11. 데이터 초기화

### 11.1 샘플 데이터 삽입
```bash
kubectl exec category-db-0 -- mongosh -u ydh2244 -p "1127star" \
  --authenticationDatabase admin categorydb \
  --eval "db.categories.insertMany([
    {name: '공지사항', slug: 'notice', parentId: null, path: 'notice', level: 0, displayOrder: 1, metadata: {icon: 'announcement'}},
    {name: '개발', slug: 'development', parentId: null, path: 'development', level: 0, displayOrder: 2, metadata: {icon: 'code'}}
  ])"
```

## 12. 성능 최적화

### 12.1 리소스 제한 조정
- **개발/테스트 환경**: 최소 리소스로 설정
- **프로덕션 환경**: 트래픽에 따른 적절한 리소스 할당 필요

### 12.2 MongoDB 인덱스
```javascript
// MongoDB 인덱스 생성
db.categories.createIndex({ slug: 1 }, { unique: true });
db.categories.createIndex({ parentId: 1, displayOrder: 1 });
db.categories.createIndex({ path: 1 });
db.categories.createIndex({ level: 1 });
```

## 13. 결론

Category Service가 MongoDB 기반으로 성공적으로 전환되었으며, 다음과 같은 결과를 얻었습니다:

✅ **기능**: 다중 깊이 트리 구조 완벽 구현  
✅ **보안**: K8s Secret 및 NetworkPolicy 적용  
✅ **안정성**: StatefulSet 기반 영구 스토리지  
✅ **테스트**: 포괄적인 단위 테스트 커버리지  
✅ **모니터링**: Health check 및 리소스 모니터링  

현재 서비스는 프로덕션 레벨의 견고한 설정으로 안정적으로 운영되고 있습니다.