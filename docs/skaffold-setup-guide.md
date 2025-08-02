# Skaffold 설치 및 개발 환경 설정 가이드

이 문서는 Signite 프로젝트의 로컬 개발 환경을 자동화하기 위해 Skaffold를 설치하고 설정하는 방법을 안내합니다. Skaffold를 사용하면 단일 명령어로 전체 마이크로서비스 스택을 빌드, 배포 및 디버깅할 수 있습니다.

## 1. Skaffold 란?

Skaffold는 Kubernetes 네이티브 애플리케이션의 개발 워크플로우를 자동화하는 경량 커맨드 라인 도구입니다. 소스 코드 변경을 감지하여 자동으로 애플리케이션을 빌드, 푸시, 배포하는 파이프라인을 처리합니다.

주요 기능:
- **자동 빌드 및 배포**: 코드 변경 시 자동으로 컨테이너 이미지를 빌드하고 Kubernetes 클러스터에 배포합니다.
- **자동 포트 포워딩**: 서비스가 배포되면 자동으로 로컬 포트로 포워딩하여 즉시 접근할 수 있도록 합니다.
- **통합 로그 스트리밍**: 모든 서비스의 로그를 하나의 터미널에서 실시간으로 확인할 수 있습니다.
- **간편한 환경 관리**: `skaffold dev` 명령 하나로 전체 개발 환경을 시작하고, `Ctrl+C`로 모든 리소스를 깔끔하게 정리합니다.

## 2. 사전 준비 사항

- **Docker Desktop**: Kubernetes 클러스터가 활성화된 Docker Desktop이 설치되어 있어야 합니다.
  - *설정 > Kubernetes > Enable Kubernetes* 체크
- **kubectl**: Kubernetes 클러스터를 제어하기 위한 `kubectl`이 설치되어 있어야 합니다. (Docker Desktop에 내장)
- **Scoop (Windows 사용자)**: Windows용 패키지 관리자인 Scoop이 설치되어 있어야 합니다.
  - Scoop 설치 (PowerShell):
    ```powershell
    Set-ExecutionPolicy RemoteSigned -scope CurrentUser
    irm get.scoop.sh | iex
    ```

## 3. Skaffold 설치

Windows 환경에서는 Scoop을 사용하여 Skaffold를 간편하게 설치할 수 있습니다.

PowerShell 또는 터미널을 열고 다음 명령어를 실행하세요.

```bash
scoop install skaffold
```

설치가 완료되면 다음 명령어를 통해 정상적으로 설치되었는지 확인합니다.

```bash
skaffold version
```

버전 정보가 출력되면 성공적으로 설치된 것입니다.

## 4. 새로운 개발 워크플로우

Skaffold가 설치되면, 프로젝트 루트 디렉터리에서 다음 단일 명령어로 전체 개발 환경을 시작할 수 있습니다.

```bash
skaffold dev
```

이 명령어를 실행하면 `skaffold.yaml` 파일의 설정에 따라 모든 서비스가 자동으로 빌드, 배포되고 포트 포워딩이 설정됩니다. 개발을 중단하려면 터미널에서 `Ctrl+C`를 입력하면 됩니다.

이제 더 이상 `dev.sh` 스크립트나 수동 `kubectl` 명령어를 사용할 필요가 없습니다.

## 5. 문제 해결

- **`skaffold: command not found`**: Skaffold가 제대로 설치되지 않았거나 시스템 PATH에 등록되지 않은 경우입니다. 설치 과정을 다시 확인해 주세요.
- **K8s 연결 오류**: `~/.kube/config` 파일이 올바르게 설정되어 있고, Docker Desktop의 Kubernetes 클러스터가 실행 중인지 확인하세요.

---
이제 Skaffold를 통해 더 빠르고 효율적인 개발을 경험해 보세요!
