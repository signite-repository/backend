FROM openjdk:17-jdk-slim

# 버전 정보 인자
ARG VERSION=unknown
ARG BUILD_DATE=unknown
ARG GIT_COMMIT=unknown

# 메타데이터 라벨
LABEL org.signite.version=${VERSION}
LABEL org.signite.build-date=${BUILD_DATE}
LABEL org.signite.git-commit=${GIT_COMMIT}
LABEL org.signite.description="Signite Backend - MSA Blog Platform"

# 작업 디렉토리 설정
WORKDIR /app

# JAR 파일 복사
COPY build/libs/*.jar app.jar

# 버전 정보를 환경변수로 설정
ENV APP_VERSION=${VERSION}
ENV BUILD_DATE=${BUILD_DATE}
ENV GIT_COMMIT=${GIT_COMMIT}

# 애플리케이션 실행
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]