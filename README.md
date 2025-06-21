# Signite

Spring Boot + Kubernetes 기반 블로그 플랫폼

## 🏗️ 아키텍처

- **Backend**: Spring Boot (Kotlin) + WebFlux + R2DBC
- **Database**: MariaDB
- **Cache**: Redis  
- **Container**: Docker
- **Orchestration**: Kubernetes

## 🚀 배포 방법

### 1. Kubernetes 클러스터에 배포

```bash
# MariaDB 배포
kubectl apply -f k8s/mariadb/

# Redis 배포  
kubectl apply -f k8s/redis/

# Signite 애플리케이션 배포
kubectl apply -f k8s/signite/
```

### 2. 포트 포워딩으로 접근

```bash
kubectl port-forward service/signite-service 8080:8080
```

### 3. API 테스트

```bash
curl http://localhost:8080/api/category
```

## 📁 프로젝트 구조

```
signite/
├── backend/                 # Spring Boot 애플리케이션
│   ├── src/main/kotlin/    # 소스 코드
│   ├── src/main/resources/ # 설정 파일
│   └── build.gradle.kts    # Gradle 빌드 설정
├── k8s/                    # Kubernetes 매니페스트
│   ├── mariadb/           # MariaDB 배포 파일
│   ├── redis/             # Redis 배포 파일
│   └── signite/           # 애플리케이션 배포 파일
└── README.md
```

## 🛠️ 개발 환경

- Java 17
- Kotlin
- Spring Boot 3.3.7
- Gradle
- Docker
- Kubernetes