# --- 1. 빌드 스테이지 (Build Stage) ---
FROM gradle:8.7-jdk17-focal AS build
WORKDIR /home/gradle/project

COPY build.gradle.kts settings.gradle.kts gradlew ./
COPY gradle ./gradle
RUN chmod +x ./gradlew

# 애플리케이션 소스 코드를 복사하기 전에 의존성을 먼저 빌드하여 캐시를 활용합니다.
RUN ./gradlew build --no-daemon -x test || true

COPY src ./src
RUN ./gradlew bootJar --no-daemon

# --- 2. 프로덕션 스테이지 (Production Stage) ---
FROM openjdk:17-jre-slim
WORKDIR /app
COPY --from=build /home/gradle/project/build/libs/post-service-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
