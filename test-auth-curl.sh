#!/bin/bash

echo "=== Testing Auth Service API ==="

# 1. Health Check
echo -e "\n1. Health Check:"
curl -X GET http://localhost:8083/actuator/health

# 2. Register with fullName
echo -e "\n\n2. Register User (with fullName):"
curl -X POST http://localhost:8083/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com", 
    "password": "password123",
    "fullName": "Test User"
  }'

# 3. Register without fullName
echo -e "\n\n3. Register User (without fullName):"
curl -X POST http://localhost:8083/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser2",
    "email": "test2@example.com",
    "password": "password123"
  }'

# 4. Login
echo -e "\n\n4. Login:"
curl -X POST http://localhost:8083/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'