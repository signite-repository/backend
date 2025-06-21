#!/bin/bash

# 🔒 Signite Kubernetes Secrets Generator
# 안전한 랜덤 패스워드로 K8s 시크릿 생성

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}🔒 Signite Kubernetes Secrets Generator${NC}"
echo

# 랜덤 패스워드 생성 함수
generate_password() {
    openssl rand -base64 32 | tr -d "=+/" | cut -c1-25
}

# Base64 인코딩 함수
encode_base64() {
    echo -n "$1" | base64 -w 0
}

# 패스워드 생성
MARIADB_ROOT_PASSWORD=$(generate_password)
MARIADB_PASSWORD=$(generate_password)
REDIS_PASSWORD=$(generate_password)
JWT_SECRET=$(generate_password)

echo -e "${YELLOW}📋 Generated passwords:${NC}"
echo -e "  MariaDB Root: ${MARIADB_ROOT_PASSWORD}"
echo -e "  MariaDB User: ${MARIADB_PASSWORD}"
echo -e "  Redis: ${REDIS_PASSWORD}"
echo -e "  JWT Secret: ${JWT_SECRET}"
echo

# MariaDB Secret 생성
echo -e "${BLUE}🗄️ Creating MariaDB secret...${NC}"
cat > k8s/mariadb/secret.yaml << EOF
# 🔒 AUTO-GENERATED - DO NOT COMMIT TO GIT
apiVersion: v1
kind: Secret
metadata:
  name: mariadb-secret
type: Opaque
data:
  mariadb-root-password: $(encode_base64 "$MARIADB_ROOT_PASSWORD")
  mariadb-password: $(encode_base64 "$MARIADB_PASSWORD")
EOF

# Redis Secret 생성
echo -e "${BLUE}📡 Creating Redis secret...${NC}"
cat > k8s/redis/secret.yaml << EOF
# 🔒 AUTO-GENERATED - DO NOT COMMIT TO GIT
apiVersion: v1
kind: Secret
metadata:
  name: redis-secret
type: Opaque
data:
  redis-password: $(encode_base64 "$REDIS_PASSWORD")
EOF

# JWT Secret 생성
echo -e "${BLUE}🔑 Creating JWT secret...${NC}"
mkdir -p k8s/secrets
cat > k8s/secrets/create-jwt-secret.yaml << EOF
# 🔒 AUTO-GENERATED - DO NOT COMMIT TO GIT
apiVersion: v1
kind: Secret
metadata:
  name: jwt-secret
type: Opaque
data:
  jwt-secret: $(encode_base64 "$JWT_SECRET")
EOF

# .env 파일 생성 (백엔드 개발용)
echo -e "${BLUE}💻 Creating backend .env file...${NC}"
cat > backend/.env << EOF
# 🔒 AUTO-GENERATED - DO NOT COMMIT TO GIT
REDIS_PASSWORD=${REDIS_PASSWORD}
JWT_SECRET=${JWT_SECRET}
DB_PASSWORD=${MARIADB_PASSWORD}
ENVIRONMENT=development
EOF

echo -e "${GREEN}✅ All secrets generated successfully!${NC}"
echo
echo -e "${YELLOW}📝 Next steps:${NC}"
echo "  1. Review the generated secrets"
echo "  2. Apply to K8s: kubectl apply -f k8s/mariadb/secret.yaml"
echo "  3. Apply to K8s: kubectl apply -f k8s/redis/secret.yaml"
echo "  4. Apply to K8s: kubectl apply -f k8s/secrets/create-jwt-secret.yaml"
echo
echo -e "${RED}⚠️  IMPORTANT: Never commit the generated secret.yaml files to Git!${NC}" 