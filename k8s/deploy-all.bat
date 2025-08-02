@echo off
echo === Starting Kubernetes Deployment ===

echo Creating Secrets...
kubectl apply -f auth-db/secret.yaml
kubectl apply -f auth-service/secret.yaml
kubectl apply -f category-db/secret.yaml
kubectl apply -f comment-db/secret.yaml
kubectl apply -f post-db/secret.yaml
kubectl apply -f websocket/secret.yaml

echo.
echo Deploying Databases...
kubectl apply -f auth-db/
kubectl apply -f category-db/
kubectl apply -f comment-db/
kubectl apply -f post-db/
kubectl apply -f post-search/
kubectl apply -f redis/
kubectl apply -f nats/

echo.
echo Waiting for databases to be ready...
timeout /t 10 /nobreak > nul

echo.
echo Deploying Services...
kubectl apply -f auth-service/
kubectl apply -f category-service/
kubectl apply -f comment-service/
kubectl apply -f post-service/
kubectl apply -f websocket/

echo.
echo Deploying Monitoring...
kubectl apply -f monitoring/

echo.
echo Deploying Istio configurations...
kubectl apply -f istio/

echo.
echo === Deployment Complete ===

echo.
echo Checking deployment status...
kubectl get pods
echo.
kubectl get services

echo.
echo === All resources deployed ===