# Category Service 트러블슈팅 가이드

## 1. 일반적인 문제 해결

### 1.1 파드 상태 확인
```bash
# 전체 파드 상태 확인
kubectl get pods | grep category

# 특정 파드 상세 정보
kubectl describe pod <pod-name>

# 파드 로그 확인
kubectl logs <pod-name> --tail=50
```

### 1.2 서비스 연결 확인
```bash
# 서비스 엔드포인트 확인
kubectl get svc | grep category
kubectl get endpoints category-service-service

# 포트 포워딩 테스트
kubectl port-forward svc/category-service-service 8081:80
curl http://localhost:8081/actuator/health
```

## 2. MongoDB 관련 문제

### 2.1 MongoDB 연결 실패
**증상**: 
```
Connection refused: category-db-service/10.103.250.137:27017
```

**원인**: 
- MongoDB 파드가 시작 중이거나 실패
- 서비스 포트 불일치
- 네트워크 정책 차단

**해결책**:
```bash
# MongoDB 파드 상태 확인
kubectl get pods | grep category-db

# MongoDB 로그 확인
kubectl logs category-db-0

# 서비스 포트 확인 (27017이어야 함)
kubectl get svc category-db-service

# MongoDB 직접 접속 테스트
kubectl exec -it category-db-0 -- mongosh -u ydh2244 -p "1127star" --authenticationDatabase admin
```

### 2.2 MongoDB OOMKilled
**증상**:
```
Last State: Terminated
Reason: OOMKilled
```

**해결책**:
```yaml
# k8s/category-db/statefulset.yaml
resources:
  requests:
    memory: "256Mi"  # 최소 256Mi 필요
  limits:
    memory: "512Mi"  # 안정적인 운영을 위해 512Mi 권장
```

### 2.3 MongoDB 인증 실패
**증상**:
```
Exception authenticating MongoCredential
Command find requires authentication
```

**해결책**:
1. Secret 값 확인:
```bash
kubectl get secret category-db-secret -o yaml
# Base64 디코딩하여 값 확인
echo "eWRoMjI0NA==" | base64 -d
```

2. 특수문자 인코딩 확인:
```bash
# @ 문자가 있으면 %40으로 인코딩 필요
# : 문자가 있으면 %3A으로 인코딩 필요
```

## 3. 애플리케이션 관련 문제

### 3.1 Spring Boot 시작 실패
**증상**:
```
Failed to instantiate Category
Type mismatch: inferred type is Map<String, Any>? but String? was expected
```

**해결책**:
1. 도메인 모델의 필드 타입 확인
2. `@Field` 어노테이션의 필드명 확인 (camelCase vs snake_case)
3. MongoDB 데이터와 Kotlin 모델 간 타입 일치 확인

### 3.2 Health Check 실패
**증상**:
```
{"status":"DOWN","mongo":{"status":"DOWN"}}
```

**해결책**:
```bash
# 1. MongoDB 연결 상태 확인
kubectl exec -it <category-service-pod> -- env | grep MONGO

# 2. 네트워크 연결 테스트
kubectl exec -it <category-service-pod> -- nslookup category-db-service

# 3. MongoDB 직접 연결 테스트
kubectl exec -it <category-service-pod> -- curl telnet://category-db-service:27017
```

### 3.3 API 응답 오류
**증상**:
```json
{"status":500,"error":"Internal Server Error"}
```

**해결책**:
```bash
# 1. 애플리케이션 로그 확인
kubectl logs <category-service-pod> --tail=100

# 2. MongoDB 데이터 확인
kubectl exec category-db-0 -- mongosh -u ydh2244 -p "1127star" --authenticationDatabase admin categorydb --eval "db.categories.find()"

# 3. 필드 매핑 오류 확인
# - parentId 타입 (String vs ObjectId)
# - metadata 타입 (Map vs String)
# - 날짜 필드 형식
```

## 4. 리소스 관련 문제

### 4.1 파드 Pending 상태
**증상**:
```
category-service-deployment-xxx   0/1   Pending
```

**해결책**:
```bash
# 1. 노드 리소스 확인
kubectl describe nodes

# 2. 파드 이벤트 확인
kubectl describe pod <pod-name>

# 3. 리소스 요청량 줄이기
# deployment.yaml에서 requests/limits 조정
```

### 4.2 이미지 Pull 실패
**증상**:
```
ErrImageNeverPull
```

**해결책**:
```bash
# 1. 이미지 빌드 확인
docker images | grep category-service

# 2. 이미지 재빌드
docker build -t signite/category-service:latest services/category-service/

# 3. imagePullPolicy 확인
# deployment.yaml에서 "Never"로 설정 확인
```

## 5. 네트워크 관련 문제

### 5.1 포트 포워딩 실패
**증상**:
```
error: lost connection to pod
unable to listen on port 8081
```

**해결책**:
```bash
# 1. 기존 포트 포워딩 프로세스 종료
lsof -ti:8081 | xargs kill -9

# 2. 파드 상태 확인 후 재시도
kubectl get pods | grep category-service
kubectl port-forward svc/category-service-service 8081:80
```

### 5.2 NetworkPolicy 차단
**증상**: 네트워크 연결이 되지 않음

**해결책**:
```bash
# 1. NetworkPolicy 확인
kubectl get networkpolicy

# 2. 임시로 NetworkPolicy 제거하여 테스트
kubectl delete networkpolicy category-db-network-policy
kubectl delete networkpolicy category-service-network-policy

# 3. 연결 확인 후 재적용
```

## 6. 데이터 관련 문제

### 6.1 한글 깨짐 현상
**증상**: API 응답에서 한글이 유니코드로 표시됨

**원인**: 터미널 인코딩 설정

**해결책**:
```bash
# 1. jq 도구 사용 (없으면 python 사용)
curl http://localhost:8081/api/v1/categories | python -m json.tool

# 2. 직접 브라우저에서 확인
# http://localhost:8081/api/v1/categories
```

### 6.2 빈 응답 또는 null 값
**증상**: API 호출 시 빈 배열 또는 null 반환

**해결책**:
```bash
# 1. MongoDB 데이터 확인
kubectl exec category-db-0 -- mongosh -u ydh2244 -p "1127star" --authenticationDatabase admin categorydb --eval "db.categories.find().pretty()"

# 2. 데이터 재입력
kubectl exec category-db-0 -- mongosh -u ydh2244 -p "1127star" --authenticationDatabase admin categorydb --eval "
db.categories.insertMany([
  {name: '공지사항', slug: 'notice', parentId: null, path: 'notice', level: 0, displayOrder: 1, metadata: {icon: 'announcement'}},
  {name: '개발', slug: 'development', parentId: null, path: 'development', level: 0, displayOrder: 2, metadata: {icon: 'code'}}
])
"
```

## 7. 성능 관련 문제

### 7.1 응답 속도 저하
**해결책**:
```bash
# 1. MongoDB 인덱스 확인
kubectl exec category-db-0 -- mongosh -u ydh2244 -p "1127star" --authenticationDatabase admin categorydb --eval "db.categories.getIndexes()"

# 2. 쿼리 성능 분석
kubectl exec category-db-0 -- mongosh -u ydh2244 -p "1127star" --authenticationDatabase admin categorydb --eval "db.categories.find({slug: 'notice'}).explain('executionStats')"

# 3. 인덱스 생성
kubectl exec category-db-0 -- mongosh -u ydh2244 -p "1127star" --authenticationDatabase admin categorydb --eval "
db.categories.createIndex({ slug: 1 }, { unique: true });
db.categories.createIndex({ parentId: 1, displayOrder: 1 });
db.categories.createIndex({ path: 1 });
"
```

## 8. 환경별 설정

### 8.1 로컬 개발 환경
```yaml
# 최소 리소스 설정
resources:
  requests:
    memory: "128Mi"
    cpu: "100m"
  limits:
    memory: "256Mi"
    cpu: "200m"
```

### 8.2 프로덕션 환경
```yaml
# 안정적인 리소스 설정
resources:
  requests:
    memory: "256Mi"
    cpu: "250m"
  limits:
    memory: "512Mi"
    cpu: "500m"
```

## 9. 유용한 명령어 모음

### 9.1 빠른 진단
```bash
# 전체 상태 한눈에 보기
kubectl get pods,svc,secrets | grep category

# 로그 실시간 모니터링
kubectl logs -f category-service-deployment-xxx

# 리소스 사용량 확인
kubectl top pods | grep category
```

### 9.2 완전 재배포
```bash
# 모든 리소스 삭제 후 재배포
kubectl delete deployment category-service-deployment
kubectl delete statefulset category-db
kubectl scale deployment category-service-deployment --replicas=0
kubectl scale deployment category-service-deployment --replicas=1
```

### 9.3 데이터 백업/복원
```bash
# MongoDB 데이터 백업
kubectl exec category-db-0 -- mongodump --authenticationDatabase admin -u ydh2244 -p "1127star" --db categorydb --out /tmp/backup

# MongoDB 데이터 복원
kubectl exec category-db-0 -- mongorestore --authenticationDatabase admin -u ydh2244 -p "1127star" --db categorydb /tmp/backup/categorydb
```

이 문서를 통해 Category Service 운영 중 발생할 수 있는 대부분의 문제를 해결할 수 있습니다.