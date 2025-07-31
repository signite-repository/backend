#!/bin/bash

echo "=== Testing Istio Gateway Configuration ==="

# Get the external IP or NodePort
INGRESS_HOST=$(kubectl get svc istio-ingressgateway -n istio-system -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
if [ -z "$INGRESS_HOST" ]; then
    echo "No LoadBalancer IP found, trying NodePort..."
    INGRESS_PORT=$(kubectl get svc istio-ingressgateway -n istio-system -o jsonpath='{.spec.ports[?(@.name=="http2")].nodePort}')
    if [ -z "$INGRESS_PORT" ]; then
        INGRESS_PORT=$(kubectl get svc istio-ingressgateway -n istio-system -o jsonpath='{.spec.ports[?(@.name=="http")].nodePort}')
    fi
    INGRESS_HOST="localhost"
    GATEWAY_URL="$INGRESS_HOST:$INGRESS_PORT"
else
    INGRESS_PORT=$(kubectl get svc istio-ingressgateway -n istio-system -o jsonpath='{.spec.ports[?(@.name=="http2")].port}')
    if [ -z "$INGRESS_PORT" ]; then
        INGRESS_PORT=$(kubectl get svc istio-ingressgateway -n istio-system -o jsonpath='{.spec.ports[?(@.name=="http")].port}')
    fi
    GATEWAY_URL="$INGRESS_HOST:$INGRESS_PORT"
fi

echo "Gateway URL: http://$GATEWAY_URL"

# Test health check endpoint
echo -e "\n1. Testing health check endpoint..."
curl -v http://$GATEWAY_URL/health

# Test category service
echo -e "\n2. Testing category service..."
curl -v http://$GATEWAY_URL/api/v1/categories

# Test post service
echo -e "\n3. Testing post service..."
curl -v http://$GATEWAY_URL/api/v1/posts

# Test comment service
echo -e "\n4. Testing comment service..."
curl -v http://$GATEWAY_URL/api/v1/comments

# Test search service
echo -e "\n5. Testing search service..."
curl -v http://$GATEWAY_URL/api/v1/search

# Check Istio proxy status
echo -e "\n=== Checking Istio Proxy Status ==="
kubectl get pods -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.spec.containers[*].name}{"\n"}{end}' | grep istio-proxy

# Debug gateway configuration
echo -e "\n=== Gateway Configuration ==="
kubectl get gateway signite-gateway -o yaml

echo -e "\n=== VirtualService Configuration ==="
kubectl get virtualservice signite-routing -o yaml