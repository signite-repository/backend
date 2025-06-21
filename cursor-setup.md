# Cursor IDE 설정 가이드

## 🚀 필수 확장 프로그램 설치

Cursor IDE에서 다음 확장 프로그램들을 설치하세요:

### Java & Spring Boot
- **Extension Pack for Java** (Microsoft)
- **Spring Boot Extension Pack** (Pivotal)
- **Spring Boot Tools** (Pivotal)
- **Spring Initializr Java Support** (Microsoft)

### Kotlin
- **Kotlin Language** (fwcd.kotlin)

### 빌드 도구
- **Gradle for Java** (Microsoft)

### 유틸리티
- **YAML** (Red Hat)
- **JSON** (Microsoft)

## ⚙️ 설정 적용 방법

1. **Cursor 설정 열기**: `Ctrl+,` (Windows) 또는 `Cmd+,` (Mac)
2. **settings.json 편집**: 우측 상단의 파일 아이콘 클릭
3. 아래 설정을 추가하거나 병합:

```json
{
    "java.configuration.updateBuildConfiguration": "automatic",
    "java.import.gradle.enabled": true,
    "java.import.gradle.wrapper.enabled": true,
    "kotlin.languageServer.enabled": true,
    "kotlin.compiler.jvm.target": "17",
    "files.encoding": "utf8",
    "files.autoGuessEncoding": true,
    "terminal.integrated.env.windows": {
        "JAVA_TOOL_OPTIONS": "-Dfile.encoding=UTF-8"
    },
    "spring-boot.ls.java.vmargs": [
        "-Dfile.encoding=UTF-8",
        "-Duser.country=KR",
        "-Duser.language=ko"
    ],
    "java.jdt.ls.vmargs": "-Xmx2G -Dfile.encoding=UTF-8",
    "[kotlin]": {
        "editor.tabSize": 4,
        "editor.insertSpaces": false
    },
    "editor.formatOnSave": true
}
```

## 🔧 문제 해결

### 1. 빨간줄 문제 해결
- **Ctrl+Shift+P** → `Java: Reload Projects`
- **Ctrl+Shift+P** → `Kotlin: Restart Language Server`
- IDE 재시작

### 2. Gradle 동기화 문제
- **Ctrl+Shift+P** → `Java: Reload Projects`
- **Ctrl+Shift+P** → `Gradle: Refresh Gradle Project`

### 3. 한글 인코딩 문제
- 이미 `gradle.properties`와 `build.gradle.kts`에 UTF-8 설정 완료
- 터미널에서 `JAVA_TOOL_OPTIONS` 환경변수 자동 설정됨

## 🏃‍♂️ 실행 방법

### 개발 서버 실행
```bash
./gradlew bootRun
```

### 빌드 (테스트 제외)
```bash
./gradlew build -x test
```

### 테스트 실행
```bash
./gradlew test
```

## 🎯 디버깅 설정

`launch.json` 파일을 생성하여 디버깅 설정:

```json
{
    "version": "0.2.0",
    "configurations": [
        {
            "type": "java",
            "name": "Spring Boot App",
            "request": "launch",
            "mainClass": "com.ydh.jigglog.JigglogBackendApplicationKt",
            "projectName": "jigglog-backend-spring",
            "vmArgs": "-Dfile.encoding=UTF-8"
        }
    ]
}
```

## ✅ 설정 완료 확인

1. **Java 버전 확인**: `java -version` (Java 17+)
2. **Gradle 버전 확인**: `./gradlew --version` (Gradle 8.5)
3. **빌드 테스트**: `./gradlew build -x test`
4. **실행 테스트**: `./gradlew bootRun`

모든 설정이 완료되면 Cursor IDE에서 Spring Boot + Kotlin 프로젝트가 원활하게 동작합니다! 