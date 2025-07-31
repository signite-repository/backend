# --- 1. 빌드 스테이지 (Build Stage) ---
# Rust 공식 이미지를 빌더로 사용합니다.
FROM rust:1.83-slim as builder

WORKDIR /usr/src/app

# 먼저 의존성 파일만 복사하여 캐싱을 활용합니다.
COPY Cargo.toml ./
# 빈 src 디렉토리와 main.rs 파일을 만들어 `cargo build --release`가 의존성만 빌드하도록 합니다.
RUN mkdir src && echo "fn main() {}" > src/main.rs
RUN cargo build --release

# 이제 실제 소스 코드를 복사합니다.
COPY src ./src

# 소스 코드를 다시 빌드합니다. 의존성은 캐시된 레이어를 사용하므로 빠르게 빌드됩니다.
RUN touch src/main.rs && cargo build --release

# --- 2. 런타임 스테이지 (Runtime Stage) ---
# 매우 가벼운 debian:bullseye-slim 이미지를 기반으로 최종 이미지를 만듭니다.
FROM debian:bookworm-slim

# SSL 라이브러리 등 런타임에 필요한 최소한의 의존성을 설치합니다.
RUN apt-get update && apt-get install -y libssl-dev ca-certificates && rm -rf /var/lib/apt/lists/*

WORKDIR /usr/src/app

# 빌더 스테이지에서 컴파일된 실행 파일을 복사합니다.
COPY --from=builder /usr/src/app/target/release/comment-service .

# 컨테이너가 시작될 때 실행할 명령을 지정합니다.
CMD ["./comment-service"]
