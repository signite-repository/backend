# 단순화된 Dockerfile
FROM gradle:8.7-jdk17-focal AS build
WORKDIR /home/gradle/project

# 프로젝트 파일들을 복사
COPY . .

# Gradle로 빌드 (wrapper 없이 직접 gradle 명령 사용)
RUN gradle bootJar --no-daemon

# --- 프로덕션 스테이지 ---
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /home/gradle/project/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]