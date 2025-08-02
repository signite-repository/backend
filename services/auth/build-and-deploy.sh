#!/bin/bash

# Build Docker image
echo "Building auth-service Docker image..."
docker build -t signite/auth-service:latest .

# Apply Kubernetes configurations
echo "Applying MariaDB configuration..."
kubectl apply -f ../../k8s/auth-db/deployment.yaml

echo "Applying Redis configuration..."
kubectl apply -f ../../k8s/redis/deployment.yaml

echo "Waiting for databases to be ready..."
kubectl wait --for=condition=ready pod -l app=auth-db --timeout=120s
kubectl wait --for=condition=ready pod -l app=redis --timeout=60s

echo "Applying auth-service configuration..."
kubectl apply -f ../../k8s/auth-service/auth-service.yaml
kubectl apply -f ../../k8s/auth-service/ingress.yaml

echo "Deployment complete!"
echo ""
echo "To test the auth service:"
echo "1. Add '127.0.0.1 auth.local' to your /etc/hosts file"
echo "2. Run: curl -X POST https://auth.local/api/auth/register -H 'Content-Type: application/json' -d '{\"username\":\"testuser\",\"email\":\"test@example.com\",\"password\":\"password123\"}'"