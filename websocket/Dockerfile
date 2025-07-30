# Build stage
FROM rust:1.82-slim as builder

# Install dependencies
RUN apt-get update && apt-get install -y \
    pkg-config \
    libssl-dev \
    && rm -rf /var/lib/apt/lists/*

# Create app directory
WORKDIR /app

# Copy manifests
COPY Cargo.toml ./

# Copy source code
COPY src ./src

# Build dependencies (this step is cached if Cargo.toml doesn't change)
RUN cargo build --release

# Runtime stage
FROM debian:bookworm-slim

# Install CA certificates and SSL libraries
RUN apt-get update && apt-get install -y \
    ca-certificates \
    libssl3 \
    && rm -rf /var/lib/apt/lists/*

# Create app user
RUN useradd -r -s /bin/false -m -d /app rust-websocket

# Set working directory
WORKDIR /app

# Copy the binary from the build stage
COPY --from=builder /app/target/release/rust-websocket-server ./

# Change ownership to app user
RUN chown -R rust-websocket:rust-websocket /app

# Switch to app user
USER rust-websocket

# Expose ports
EXPOSE 8080 3001

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:3001/health || exit 1

# Set environment variables
ENV RUST_LOG=rust_websocket_server=info,info
ENV SERVER_HOST=0.0.0.0
ENV SERVER_PORT=8080
ENV HTTP_PORT=3001

# Run the binary
CMD ["./rust-websocket-server"] 