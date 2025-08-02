# Auth Service API Test Script (Port 8083)

Write-Host "=== Auth Service API Test (Port 8083) ===" -ForegroundColor Green

# 1. Health Check
Write-Host "`n1. Testing Health Check..." -ForegroundColor Yellow
try {
    $health = Invoke-WebRequest -Uri http://localhost:8083/actuator/health -Method GET -UseBasicParsing
    Write-Host "Health Check: OK" -ForegroundColor Green
    Write-Host $health.Content
} catch {
    Write-Host "Health Check: FAILED" -ForegroundColor Red
    Write-Host $_.Exception.Message
}

# 2. Register User
Write-Host "`n2. Testing User Registration..." -ForegroundColor Yellow
$registerBody = @{
    username = "testuser"
    password = "password123"
    email = "test@example.com"
} | ConvertTo-Json

try {
    $register = Invoke-WebRequest -Uri http://localhost:8083/api/auth/register `
        -Method POST `
        -ContentType "application/json" `
        -Body $registerBody `
        -UseBasicParsing
    
    Write-Host "Registration: OK" -ForegroundColor Green
    Write-Host $register.Content
} catch {
    Write-Host "Registration: FAILED" -ForegroundColor Red
    Write-Host $_.Exception.Message
    if ($_.Exception.Response) {
        $reader = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response Body:" -ForegroundColor Red
        Write-Host $responseBody
    }
}

# 3. Login
Write-Host "`n3. Testing Login..." -ForegroundColor Yellow
$loginBody = @{
    username = "testuser"
    password = "password123"
} | ConvertTo-Json

try {
    $login = Invoke-WebRequest -Uri http://localhost:8083/api/auth/login `
        -Method POST `
        -ContentType "application/json" `
        -Body $loginBody `
        -UseBasicParsing
    
    Write-Host "Login: OK" -ForegroundColor Green
    $loginResponse = $login.Content | ConvertFrom-Json
    Write-Host $login.Content
    
    # Extract JWT token
    $token = $loginResponse.token
    Write-Host "`nJWT Token received: $($token.Substring(0, 20))..." -ForegroundColor Cyan
    
    # 4. Get Profile with JWT
    Write-Host "`n4. Testing Get Profile..." -ForegroundColor Yellow
    try {
        $headers = @{
            "Authorization" = "Bearer $token"
        }
        
        $profile = Invoke-WebRequest -Uri http://localhost:8083/api/auth/profile `
            -Method GET `
            -Headers $headers `
            -UseBasicParsing
        
        Write-Host "Get Profile: OK" -ForegroundColor Green
        Write-Host $profile.Content
    } catch {
        Write-Host "Get Profile: FAILED" -ForegroundColor Red
        Write-Host $_.Exception.Message
    }
    
} catch {
    Write-Host "Login: FAILED" -ForegroundColor Red
    Write-Host $_.Exception.Message
    if ($_.Exception.Response) {
        $reader = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response Body:" -ForegroundColor Red
        Write-Host $responseBody
    }
}

Write-Host "`n=== Test Complete ===" -ForegroundColor Green