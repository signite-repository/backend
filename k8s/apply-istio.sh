#!/bin/bash

echo "Applying Istio configuration..."

# Apply Istio gateway and routing rules
kubectl apply -f istio/routing.yaml
kubectl apply -f istio/authentication.yaml
kubectl apply -f istio/envoy-filter.yaml
kubectl apply -f istio/destination-rules.yaml
kubectl apply -f istio/gateway-service.yaml

# Apply service configurations
kubectl apply -f category-db/
kubectl apply -f category-service/
kubectl apply -f post-db/
kubectl apply -f post-service/
kubectl apply -f comment-db/
kubectl apply -f comment-service/
kubectl apply -f post-search/

echo "Waiting for services to be ready..."
sleep 10

# Check Istio gateway status
echo -e "\n=== Istio Gateway Status ==="
kubectl get gateway,virtualservice -n default

echo -e "\n=== Istio Ingress Gateway ==="
kubectl get svc istio-ingressgateway -n istio-system

echo -e "\n=== Services with Istio sidecar ==="
kubectl get pods -l sidecar.istio.io/inject=true

echo -e "\n=== All Services ==="
kubectl get svc

echo -e "\n=== Testing endpoints ==="
GATEWAY_URL=$(kubectl get svc istio-ingressgateway -n istio-system -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
if [ -z "$GATEWAY_URL" ]; then
    GATEWAY_URL=$(kubectl get svc istio-ingressgateway -n istio-system -o jsonpath='{.spec.ports[?(@.name=="http2")].nodePort}')
    GATEWAY_URL="localhost:$GATEWAY_URL"
fi

echo "Gateway URL: $GATEWAY_URL"
echo "Testing /api/v1/categories endpoint..."
curl -i http://$GATEWAY_URL/api/v1/categories || echo "Failed to connect"